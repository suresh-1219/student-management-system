package com.suresh.sms.batch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.entity.ReportExport;
import com.suresh.sms.entity.ReportExportStatus;
import com.suresh.sms.exception.StorageException;
import com.suresh.sms.repository.ReportExportRepository;
import com.suresh.sms.service.StorageService;

/**
 * Bridges the batch job's world (a {@link JobExecution}, a CSV file sitting
 * on local disk) back to this app's own world (a {@link ReportExport} row a
 * client is polling, a file that belongs in object storage, not on this
 * server's disk). {@link StudentExportBatchConfig}'s step only produces the
 * local file; everything after that - upload, bookkeeping, local cleanup -
 * happens here, once, after the job either finishes or fails.
 */
@Component
public class ReportExportJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ReportExportJobListener.class);

    @Autowired
    private ReportExportRepository repository;

    @Autowired
    private StorageService storageService;

    @Override
    @Transactional
    public void beforeJob(JobExecution jobExecution) {

        findExport(jobExecution).ifPresent(export -> {
            export.setStatus(ReportExportStatus.RUNNING);
            repository.save(export);
        });
    }

    @Override
    @Transactional
    public void afterJob(JobExecution jobExecution) {

        ReportExport export = findExport(jobExecution).orElse(null);

        if (export == null) {
            // Shouldn't happen (ReportExportService always creates the row
            // before launching the job), but a missing row is not worth
            // crashing the listener over - just leave nothing to update.
            log.warn("No ReportExport row found for job execution {}", jobExecution.getId());
            return;
        }

        String filePath = jobExecution.getJobParameters().getString("filePath");

        try {
            if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                completeExport(export, filePath);
            } else {
                failExport(export, jobExecution);
            }
        } finally {
            deleteQuietly(filePath);
        }
    }

    private void completeExport(ReportExport export, String filePath) {

        Path path = Path.of(filePath);
        String filename = path.getFileName().toString();
        String objectKey = "report-exports/" + export.getId() + "/" + filename;

        try {
            long size = Files.size(path);

            try (InputStream data = Files.newInputStream(path)) {
                storageService.upload(objectKey, data, size, "text/csv");
            }

            export.setStatus(ReportExportStatus.COMPLETED);
            export.setObjectKey(objectKey);
            export.setFilename(filename);
            export.setSizeBytes(size);
            export.setCompletedAt(LocalDateTime.now());

        } catch (IOException | StorageException ex) {
            // The batch step itself succeeded, but we couldn't get the
            // result to the client - that's a failed export from their
            // point of view, same as a step failure.
            log.error("Report export {} finished but could not be uploaded", export.getId(), ex);

            export.setStatus(ReportExportStatus.FAILED);
            export.setErrorMessage("Export finished but could not be uploaded to object storage");
            export.setCompletedAt(LocalDateTime.now());
        }

        repository.save(export);
    }

    private void failExport(ReportExport export, JobExecution jobExecution) {

        String message = jobExecution.getAllFailureExceptions().stream()
                .findFirst()
                .map(Throwable::getMessage)
                .orElse("Export job did not complete successfully (status: "
                        + jobExecution.getStatus() + ")");

        log.error("Report export {} failed: {}", export.getId(), message);

        export.setStatus(ReportExportStatus.FAILED);
        export.setErrorMessage(message);
        export.setCompletedAt(LocalDateTime.now());

        repository.save(export);
    }

    /** Best-effort: a leftover temp file is a disk-space nuisance, not a request failure. */
    private void deleteQuietly(String filePath) {

        if (filePath == null) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException ex) {
            log.warn("Could not delete temporary export file '{}'", filePath, ex);
        }
    }

    private Optional<ReportExport> findExport(JobExecution jobExecution) {

        Long exportId = jobExecution.getJobParameters().getLong("reportExportId");

        if (exportId == null) {
            return Optional.empty();
        }

        return repository.findById(exportId);
    }
}
