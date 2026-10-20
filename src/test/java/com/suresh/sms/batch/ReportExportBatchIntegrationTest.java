package com.suresh.sms.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.suresh.sms.entity.ReportExport;
import com.suresh.sms.entity.ReportExportStatus;
import com.suresh.sms.entity.ReportExportType;
import com.suresh.sms.entity.Student;
import com.suresh.sms.repository.ReportExportRepository;
import com.suresh.sms.repository.StudentRepository;
import com.suresh.sms.service.StorageService;
import com.suresh.sms.support.AbstractIntegrationTest;

/**
 * Drives {@code studentExportJob} end to end - real {@link Student} rows in
 * a real database, a real CSV chunk-written by Spring Batch, a real upload
 * to a real S3-compatible server, exactly the path {@code
 * ReportExportService} triggers in production. {@code
 * ReportExportJobListenerTest} and {@code ReportExportServiceTest} already
 * cover the surrounding bookkeeping against mocks; this is the one test
 * that proves the reader/writer/listener chain actually produces a correct
 * file and lands it in object storage, not just that the right calls are
 * made.
 *
 * <p>Needs its own SeaweedFS container, in addition to the MySQL container
 * {@link AbstractIntegrationTest} already provides - a report export reads
 * from one and uploads to the other. See {@code StorageServiceIntegrationTest}
 * for why SeaweedFS rather than MinIO, and why a plain {@link
 * GenericContainer} rather than a Testcontainers module (there isn't one for
 * SeaweedFS).
 */
@Testcontainers
@SpringBootTest
@SpringBatchTest
class ReportExportBatchIntegrationTest extends AbstractIntegrationTest {

    private static final String ACCESS_KEY = "report-it-access-key";
    private static final String SECRET_KEY = "report-it-secret-key";

    private static final String S3_IDENTITY_CONFIG = """
            {
              "identities": [
                {
                  "name": "it",
                  "credentials": [
                    { "accessKey": "%s", "secretKey": "%s" }
                  ],
                  "actions": ["Admin", "Read", "Write", "List", "Tagging"]
                }
              ]
            }
            """.formatted(ACCESS_KEY, SECRET_KEY);

    private static final String STARTUP_SCRIPT =
            "mkdir -p /etc/seaweedfs && cat > /etc/seaweedfs/s3.json <<'EOF'\n"
                    + S3_IDENTITY_CONFIG
                    + "\nEOF\n"
                    + "exec weed server -dir=/data -s3 -s3.port=8333 -s3.config=/etc/seaweedfs/s3.json";

    // Own container, own bucket, own credentials - kept entirely separate
    // from StorageServiceIntegrationTest's so the two test classes never
    // share state (Testcontainers gives each static container its own
    // randomly-mapped host port regardless).
    static final GenericContainer<?> SEAWEEDFS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("chrislusf/seaweedfs:latest"))
                    .withExposedPorts(8333)
                    .withCreateContainerCmdModifier(
                            cmd -> cmd.withEntrypoint("/bin/sh", "-c"))
                    .withCommand(new String[] { STARTUP_SCRIPT })
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(90)));

    static {
        SEAWEEDFS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void s3Properties(DynamicPropertyRegistry registry) {
        registry.add("app.s3.endpoint", ReportExportBatchIntegrationTest::endpoint);
        registry.add("app.s3.access-key", () -> ACCESS_KEY);
        registry.add("app.s3.secret-key", () -> SECRET_KEY);
        registry.add("app.s3.bucket", () -> "report-exports-it");
    }

    private static String endpoint() {
        return "http://" + SEAWEEDFS_CONTAINER.getHost() + ":"
                + SEAWEEDFS_CONTAINER.getMappedPort(8333);
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ReportExportRepository reportExportRepository;

    @Autowired
    private StorageService storageService;

    @Test
    void studentExportJob_runsEndToEndAndUploadsRealCsv() throws Exception {

        Student alice = studentRepository.save(new Student(
                null, "Alice Integration", "alice.it@example.com", "CS101", new BigDecimal("500.00")));
        Student bob = studentRepository.save(new Student(
                null, "Bob Integration", "bob.it@example.com", "CS102", new BigDecimal("600.00")));

        ReportExport export = reportExportRepository.save(
                new ReportExport(ReportExportType.STUDENTS, ReportExportStatus.PENDING, "admin"));

        Path filePath = Files.createTempFile("report-export-it-", ".csv");
        // The writer itself must create the file; this call only reserves a
        // unique, guaranteed-not-pre-existing name for it.
        Files.deleteIfExists(filePath);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("reportExportId", export.getId())
                .addString("filePath", filePath.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // BatchConfig wires the same async JobLauncher used in production
        // (see its Javadoc), so launchJob() can return before the job - and
        // ReportExportJobListener#afterJob - have actually finished.
        //
        // Deliberately polling the report_exports ROW here, not
        // jobExecution.getStatus(): Spring Batch flips a JobExecution's own
        // BatchStatus to COMPLETED *inside* doExecute(), a moment before
        // AbstractJob calls afterJob() - the listener that actually writes
        // this row - so waiting on the JobExecution's status instead of the
        // row it predicts can observe COMPLETED before the write has
        // happened. Polling the row is both the fix and the more honest
        // test: it's exactly what a real client hitting
        // GET /reports/exports/{id} repeatedly would see.
        ReportExport completed = awaitTerminalStatus(export.getId());

        assertEquals(ReportExportStatus.COMPLETED, completed.getStatus());
        // By the time the row says COMPLETED, the listener (same thread,
        // same method call) has already run past the point where Spring
        // Batch set this, so it's safe to assert on it now too.
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertNotNull(completed.getObjectKey());
        assertNotNull(completed.getFilename());
        assertNotNull(completed.getSizeBytes());

        // The listener deletes the local staging file once it's uploaded -
        // if it's still there, the "upload" didn't really happen.
        assertTrue(Files.notExists(filePath), "local staging file should be cleaned up after upload");

        String csv = downloadViaPresignedUrl(completed.getObjectKey());
        assertTrue(csv.startsWith("id,name,email,course,fee,createdAt"));
        assertTrue(csv.contains(alice.getEmail()));
        assertTrue(csv.contains(bob.getEmail()));
    }

    private ReportExport awaitTerminalStatus(Long exportId) throws InterruptedException {

        long deadline = System.currentTimeMillis() + Duration.ofSeconds(60).toMillis();
        ReportExport export;

        do {
            export = reportExportRepository.findById(exportId).orElseThrow();

            if (export.getStatus() == ReportExportStatus.COMPLETED
                    || export.getStatus() == ReportExportStatus.FAILED) {
                return export;
            }

            Thread.sleep(200);
        } while (System.currentTimeMillis() < deadline);

        // Timed out - return whatever the row still says (PENDING/RUNNING)
        // so the assertion that follows reports that, instead of this
        // method throwing a less informative timeout exception of its own.
        return export;
    }

    private String downloadViaPresignedUrl(String objectKey) throws Exception {

        String url = storageService.presignedDownloadUrl(objectKey);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        assertEquals(200, connection.getResponseCode());

        try (var in = connection.getInputStream()) {
            return new String(in.readAllBytes());
        }
    }
}
