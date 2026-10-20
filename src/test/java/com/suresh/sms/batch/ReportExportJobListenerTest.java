package com.suresh.sms.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.MetaDataInstanceFactory;

import com.suresh.sms.entity.ReportExport;
import com.suresh.sms.entity.ReportExportStatus;
import com.suresh.sms.entity.ReportExportType;
import com.suresh.sms.exception.StorageException;
import com.suresh.sms.repository.ReportExportRepository;
import com.suresh.sms.service.StorageService;

@ExtendWith(MockitoExtension.class)
class ReportExportJobListenerTest {

    @Mock
    private ReportExportRepository repository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ReportExportJobListener listener;

    private Path tempFile;

    @AfterEach
    void cleanup() throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    private ReportExport export(Long id, ReportExportStatus status) {
        ReportExport export = new ReportExport(ReportExportType.STUDENTS, status, "admin");
        org.springframework.test.util.ReflectionTestUtils.setField(export, "id", id);
        return export;
    }

    private JobExecution jobExecution(Long exportId, String filePath) {

        JobParameters params = new JobParametersBuilder()
                .addLong("reportExportId", exportId)
                .addString("filePath", filePath)
                .toJobParameters();

        return MetaDataInstanceFactory.createJobExecution(
                "studentExportJob", exportId, exportId, params);
    }

    @Test
    void beforeJob_marksExportRunning() {

        ReportExport export = export(1L, ReportExportStatus.PENDING);
        when(repository.findById(1L)).thenReturn(Optional.of(export));

        listener.beforeJob(jobExecution(1L, "/tmp/irrelevant.csv"));

        assertEquals(ReportExportStatus.RUNNING, export.getStatus());
        verify(repository).save(export);
    }

    @Test
    void beforeJob_whenExportRowMissing_doesNothing() {

        when(repository.findById(404L)).thenReturn(Optional.empty());

        listener.beforeJob(jobExecution(404L, "/tmp/irrelevant.csv"));

        verify(repository, never()).save(any());
    }

    @Test
    void afterJob_whenCompleted_uploadsFileAndMarksCompleted() throws IOException {

        tempFile = Files.createTempFile("sms-report-export-test", ".csv");
        Files.writeString(tempFile, "id,name\n1,Alice\n");

        ReportExport export = export(2L, ReportExportStatus.RUNNING);
        when(repository.findById(2L)).thenReturn(Optional.of(export));

        JobExecution jobExecution = jobExecution(2L, tempFile.toString());
        jobExecution.setStatus(BatchStatus.COMPLETED);

        listener.afterJob(jobExecution);

        verify(storageService).upload(
                eq("report-exports/2/" + tempFile.getFileName()),
                any(),
                anyLong(),
                eq("text/csv"));

        assertEquals(ReportExportStatus.COMPLETED, export.getStatus());
        assertEquals(tempFile.getFileName().toString(), export.getFilename());
        assertNotNull(export.getSizeBytes());
        assertNotNull(export.getCompletedAt());
        verify(repository).save(export);

        assertFalse(Files.exists(tempFile), "local temp file should be deleted after a successful upload");
    }

    @Test
    void afterJob_whenCompletedButUploadFails_marksFailed() throws IOException {

        tempFile = Files.createTempFile("sms-report-export-test", ".csv");
        Files.writeString(tempFile, "id,name\n1,Alice\n");

        ReportExport export = export(3L, ReportExportStatus.RUNNING);
        when(repository.findById(3L)).thenReturn(Optional.of(export));
        doThrow(new StorageException("object store unreachable", new RuntimeException()))
                .when(storageService).upload(anyString(), any(), anyLong(), anyString());

        JobExecution jobExecution = jobExecution(3L, tempFile.toString());
        jobExecution.setStatus(BatchStatus.COMPLETED);

        listener.afterJob(jobExecution);

        assertEquals(ReportExportStatus.FAILED, export.getStatus());
        assertNotNull(export.getErrorMessage());
        verify(repository).save(export);

        assertFalse(Files.exists(tempFile), "local temp file should still be cleaned up after a failed upload");
    }

    @Test
    void afterJob_whenJobFailed_marksExportFailedWithFailureMessage() throws IOException {

        tempFile = Files.createTempFile("sms-report-export-test", ".csv");

        ReportExport export = export(4L, ReportExportStatus.RUNNING);
        when(repository.findById(4L)).thenReturn(Optional.of(export));

        JobExecution jobExecution = jobExecution(4L, tempFile.toString());
        jobExecution.setStatus(BatchStatus.FAILED);
        jobExecution.addFailureException(new RuntimeException("database connection lost"));

        listener.afterJob(jobExecution);

        assertEquals(ReportExportStatus.FAILED, export.getStatus());
        assertTrue(export.getErrorMessage().contains("database connection lost"));
        verify(storageService, never()).upload(anyString(), any(), anyLong(), anyString());
        verify(repository).save(export);

        assertFalse(Files.exists(tempFile), "local temp file should be cleaned up even when the job failed");
    }

    @Test
    void afterJob_whenExportRowMissing_logsAndDoesNotThrow() {

        when(repository.findById(999L)).thenReturn(Optional.empty());

        JobExecution jobExecution = jobExecution(999L, "/tmp/does-not-matter.csv");
        jobExecution.setStatus(BatchStatus.COMPLETED);

        listener.afterJob(jobExecution);

        verify(repository, never()).save(any());
    }
}
