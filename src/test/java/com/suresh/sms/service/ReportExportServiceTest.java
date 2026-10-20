package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.suresh.sms.dto.ReportDownloadDTO;
import com.suresh.sms.dto.ReportExportDTO;
import com.suresh.sms.entity.ReportExport;
import com.suresh.sms.entity.ReportExportStatus;
import com.suresh.sms.entity.ReportExportType;
import com.suresh.sms.exception.ReportExportNotFoundException;
import com.suresh.sms.exception.ReportExportNotReadyException;
import com.suresh.sms.repository.ReportExportRepository;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @Mock
    private ReportExportRepository repository;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job studentExportJob;

    @Mock
    private StorageService storageService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ReportExportService service;

    @BeforeEach
    void stubTransactionTemplate() {

        // ReportExportService deliberately runs its row-creation/failure
        // bookkeeping through TransactionTemplate rather than
        // @Transactional on a same-class helper (see the class Javadoc) -
        // this makes the mock behave like a real one by actually invoking
        // the callback it's handed.
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback =
                    invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });

        ReflectionTestUtils.setField(service, "exportDir",
                System.getProperty("java.io.tmpdir") + "/sms-report-exports-test");
    }

    private ReportExport pendingExport(Long id) {
        ReportExport export = new ReportExport(ReportExportType.STUDENTS, ReportExportStatus.PENDING, "admin");
        ReflectionTestUtils.setField(export, "id", id);
        return export;
    }

    @Test
    void requestStudentExport_createsRowAndLaunchesJob() throws Exception {

        when(repository.save(any(ReportExport.class))).thenReturn(pendingExport(1L));

        ReportExportDTO result = service.requestStudentExport("admin");

        assertEquals(1L, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals("STUDENTS", result.getExportType());

        verify(jobLauncher).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void requestStudentExport_passesExportIdAndFilePathAsJobParameters() throws Exception {

        when(repository.save(any(ReportExport.class))).thenReturn(pendingExport(42L));

        service.requestStudentExport("admin");

        org.mockito.ArgumentCaptor<JobParameters> captor =
                org.mockito.ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(any(Job.class), captor.capture());

        JobParameters params = captor.getValue();
        assertEquals(42L, params.getLong("reportExportId"));
        assertTrue(params.getString("filePath").endsWith("export-42.csv"));
    }

    @Test
    void requestStudentExport_whenJobLauncherFails_marksExportFailed() throws Exception {

        when(repository.save(any(ReportExport.class)))
                .thenReturn(pendingExport(2L))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(2L)).thenReturn(Optional.of(pendingExport(2L)));
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("already running"));

        ReportExportDTO result = service.requestStudentExport("admin");

        assertEquals("FAILED", result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Could not start the export job"));
    }

    @Test
    void requestStudentExport_whenExportDirCannotBeCreated_marksExportFailed() throws Exception {

        // Point the export directory at a path whose PARENT is a plain file,
        // not a directory - Files.createDirectories(...) then fails with a
        // real IOException, no static-method mocking required.
        File blocker = File.createTempFile("sms-report-export-blocker", "");
        blocker.deleteOnExit();

        Path bogusDir = blocker.toPath().resolve("subdir");
        ReflectionTestUtils.setField(service, "exportDir", bogusDir.toString());

        when(repository.save(any(ReportExport.class)))
                .thenReturn(pendingExport(3L))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(3L)).thenReturn(Optional.of(pendingExport(3L)));

        ReportExportDTO result = service.requestStudentExport("admin");

        assertEquals("FAILED", result.getStatus());
        verify(jobLauncher, never()).run(any(), any());
    }

    @Test
    void getExport_notFound_throws() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReportExportNotFoundException.class, () -> service.getExport(99L));
    }

    @Test
    void getExport_found_returnsDto() {

        ReportExport export = pendingExport(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(export));

        ReportExportDTO result = service.getExport(5L);

        assertEquals(5L, result.getId());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void getExportDownload_notFound_throws() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReportExportNotFoundException.class, () -> service.getExportDownload(99L));
    }

    @Test
    void getExportDownload_whenNotCompleted_throwsNotReady() {

        ReportExport export = pendingExport(6L);
        when(repository.findById(6L)).thenReturn(Optional.of(export));

        assertThrows(ReportExportNotReadyException.class, () -> service.getExportDownload(6L));
    }

    @Test
    void getExportDownload_whenCompleted_returnsPresignedUrl() {

        ReportExport export = pendingExport(7L);
        export.setStatus(ReportExportStatus.COMPLETED);
        export.setObjectKey("report-exports/7/export-7.csv");
        export.setFilename("export-7.csv");
        export.setSizeBytes(123L);

        when(repository.findById(7L)).thenReturn(Optional.of(export));
        when(storageService.presignedDownloadUrl("report-exports/7/export-7.csv"))
                .thenReturn("https://objectstore.local/presigned");
        when(storageService.getPresignedUrlExpirySeconds()).thenReturn(600);

        ReportDownloadDTO result = service.getExportDownload(7L);

        assertEquals("export-7.csv", result.getFilename());
        assertEquals(123L, result.getSizeBytes());
        assertEquals("https://objectstore.local/presigned", result.getDownloadUrl());
        assertEquals(600, result.getExpiresInSeconds());
    }
}
