package com.suresh.sms.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.suresh.sms.dto.ReportDownloadDTO;
import com.suresh.sms.dto.ReportExportDTO;
import com.suresh.sms.entity.ReportExport;
import com.suresh.sms.entity.ReportExportStatus;
import com.suresh.sms.entity.ReportExportType;
import com.suresh.sms.exception.ReportExportNotFoundException;
import com.suresh.sms.exception.ReportExportNotReadyException;
import com.suresh.sms.repository.ReportExportRepository;

/**
 * Triggers and tracks CSV report exports (Drop G3). The actual work -
 * reading students, writing the CSV, uploading it - happens in {@code
 * StudentExportBatchConfig}'s batch job and {@code ReportExportJobListener};
 * this class is just the front door: create the tracking row, hand the job
 * to the (async) {@link JobLauncher}, and answer status/download questions
 * about rows the listener updates later.
 *
 * <p>Deliberately NOT one {@code @Transactional} method end to end, and
 * deliberately using an explicit {@link TransactionTemplate} rather than
 * {@code @Transactional} on a private helper: {@link #requestStudentExport}
 * must COMMIT the {@code PENDING} row before handing the job to the
 * launcher, not merely write it to an open transaction - the job runs on
 * its own thread (see {@code BatchConfig}) almost immediately, and {@code
 * ReportExportJobListener#beforeJob} needs to find that row from a
 * connection of its own. A same-class call to an {@code @Transactional}
 * method doesn't go through Spring's proxy, so it would silently run in
 * whatever transaction (if any) is already open - exactly the race this is
 * avoiding - which is why this uses {@link TransactionTemplate#execute}
 * instead.
 */
@Service
public class ReportExportService {

    private static final Logger log = LoggerFactory.getLogger(ReportExportService.class);

    @Autowired
    private ReportExportRepository repository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job studentExportJob;

    @Autowired
    private StorageService storageService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // Where the CSV is staged locally before ReportExportJobListener uploads
    // it and deletes it again - never served to clients directly.
    @Value("${app.reports.export-dir:${java.io.tmpdir}/sms-report-exports}")
    private String exportDir;

    public ReportExportDTO requestStudentExport(String requestedBy) {

        ReportExport export = transactionTemplate.execute(status ->
                repository.save(new ReportExport(
                        ReportExportType.STUDENTS, ReportExportStatus.PENDING, requestedBy)));

        Path filePath = Path.of(exportDir, "export-" + export.getId() + ".csv");

        try {
            Files.createDirectories(filePath.getParent());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("reportExportId", export.getId())
                    .addString("filePath", filePath.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(studentExportJob, jobParameters);

        } catch (IOException | JobExecutionException ex) {
            log.error("Could not start report export {}", export.getId(), ex);
            export = markFailedToStart(export.getId(), ex.getMessage());
        }

        return convertToDTO(export);
    }

    private ReportExport markFailedToStart(Long exportId, String reason) {

        return transactionTemplate.execute(status -> {

            ReportExport export = repository.findById(exportId)
                    .orElseThrow(() -> new ReportExportNotFoundException(
                            "Report export not found with id : " + exportId));

            export.setStatus(ReportExportStatus.FAILED);
            export.setErrorMessage("Could not start the export job: " + reason);
            export.setCompletedAt(LocalDateTime.now());

            return repository.save(export);
        });
    }

    @Transactional(readOnly = true)
    public ReportExportDTO getExport(Long id) {
        return convertToDTO(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public ReportDownloadDTO getExportDownload(Long id) {

        ReportExport export = findOrThrow(id);

        if (!export.isCompleted()) {
            throw new ReportExportNotReadyException(
                    "Report export " + id + " is not ready for download (status: "
                            + export.getStatus() + ")");
        }

        String url = storageService.presignedDownloadUrl(export.getObjectKey());

        return new ReportDownloadDTO(
                export.getFilename(),
                export.getSizeBytes(),
                url,
                storageService.getPresignedUrlExpirySeconds());
    }

    private ReportExport findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ReportExportNotFoundException(
                        "Report export not found with id : " + id));
    }

    private ReportExportDTO convertToDTO(ReportExport export) {

        ReportExportDTO dto = new ReportExportDTO();
        dto.setId(export.getId());
        dto.setExportType(export.getExportType().name());
        dto.setStatus(export.getStatus().name());
        dto.setFilename(export.getFilename());
        dto.setSizeBytes(export.getSizeBytes());
        dto.setErrorMessage(export.getErrorMessage());
        dto.setCreatedAt(export.getCreatedAt());
        dto.setCompletedAt(export.getCompletedAt());

        return dto;
    }
}
