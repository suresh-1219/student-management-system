package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.suresh.sms.exception.StorageException;
import com.suresh.sms.support.AbstractIntegrationTest;

import io.minio.MinioClient;

/**
 * Exercises {@link StorageService} against a real S3-compatible server in
 * Docker, the same rigor {@link AbstractIntegrationTest} gives MySQL -
 * unlike the mocked-client unit tests in {@code StorageServiceTest}, this
 * proves the upload/presigned-URL/delete round trip actually works, not
 * just that the right SDK calls are made.
 *
 * <p>The server is <a href="https://github.com/seaweedfs/seaweedfs">
 * SeaweedFS</a>, not MinIO: this feature was originally built and tested
 * against a real MinIO server, but MinIO discontinued free container
 * images (pulled from both Docker Hub and quay.io) and its last free
 * release has an unpatched critical CVE, making it unusable for this
 * purpose. There is no Testcontainers module for SeaweedFS (unlike
 * MySQL, or MinIO's own now-moot one), so this starts a plain
 * {@link GenericContainer} running SeaweedFS's all-in-one server with its
 * S3 gateway enabled, writing the same identity/credentials config
 * docker-compose.yml's seaweedfs service writes, and wires the resulting
 * endpoint in by hand via {@code @DynamicPropertySource} (Spring Boot's
 * {@code @ServiceConnection} has no built-in support for this either).
 */
@Testcontainers
@SpringBootTest
class StorageServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String ACCESS_KEY = "it-access-key";
    private static final String SECRET_KEY = "it-secret-key";

    // Same shape as the config docker-compose.yml's seaweedfs service
    // writes - a single identity with full access, used only by this test.
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

    // "latest": SeaweedFS has no long-term-support tag convention this
    // project can safely pin to from here, and the MinIO saga this
    // replaces is a direct lesson in pinned-tag rot. If this image is slow
    // to pull the first time, run `docker pull chrislusf/seaweedfs:latest`
    // once beforehand so Testcontainers' own (non-configurable, ~2 minute)
    // pull-wait doesn't time out on a slow connection.
    static final GenericContainer<?> SEAWEEDFS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("chrislusf/seaweedfs:latest"))
                    .withExposedPorts(8333)
                    // GenericContainer has no withEntrypoint(...) of its own
                    // (that's only on docker-java's CreateContainerCmd) - this
                    // is the standard Testcontainers way to override the
                    // image's entrypoint.
                    .withCreateContainerCmdModifier(
                            cmd -> cmd.withEntrypoint("/bin/sh", "-c"))
                    .withCommand(new String[] { STARTUP_SCRIPT })
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(90)));

    static {
        SEAWEEDFS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void s3Properties(DynamicPropertyRegistry registry) {
        registry.add("app.s3.endpoint", StorageServiceIntegrationTest::endpoint);
        registry.add("app.s3.access-key", () -> ACCESS_KEY);
        registry.add("app.s3.secret-key", () -> SECRET_KEY);
        registry.add("app.s3.bucket", () -> "course-syllabi-it");
    }

    private static String endpoint() {
        return "http://" + SEAWEEDFS_CONTAINER.getHost() + ":"
                + SEAWEEDFS_CONTAINER.getMappedPort(8333);
    }

    @Autowired
    private StorageService storageService;

    @Test
    void testUploadThenPresignedUrlThenDeleteRoundTrip() throws Exception {

        String objectKey = "it/" + System.nanoTime() + "/syllabus.txt";
        byte[] content = "a real syllabus for integration testing".getBytes(StandardCharsets.UTF_8);

        try (InputStream data = new ByteArrayInputStream(content)) {
            storageService.upload(objectKey, data, content.length, "text/plain");
        }

        String url = storageService.presignedDownloadUrl(objectKey);

        // The presigned URL isn't just well-formed - it actually works
        // against the real server, with the real bytes behind it.
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        assertEquals(200, connection.getResponseCode());
        byte[] downloaded = connection.getInputStream().readAllBytes();
        assertEquals(
                new String(content, StandardCharsets.UTF_8),
                new String(downloaded, StandardCharsets.UTF_8));

        storageService.delete(objectKey);

        // Deleted for real: a fresh presigned URL for the same key now
        // points at nothing, so fetching it 404s.
        String urlAfterDelete = storageService.presignedDownloadUrl(objectKey);
        HttpURLConnection afterDelete = (HttpURLConnection) new URL(urlAfterDelete).openConnection();
        afterDelete.setRequestMethod("GET");
        assertEquals(404, afterDelete.getResponseCode());
    }

    @Test
    void testDeleteOfUnknownObjectDoesNotThrow() {

        // S3-compatible semantics: deleting a key that was never there is
        // not an error.
        storageService.delete("it/does-not-exist");
    }

    @Test
    void testOperationsOnMissingBucketFailFast() {

        // A sibling StorageService instance pointed at a bucket that was
        // never created proves failures really do surface as
        // StorageException, not get swallowed.
        StorageService misconfigured = new StorageService();
        ReflectionTestUtils.setField(
                misconfigured, "minioClient",
                MinioClient.builder()
                        .endpoint(endpoint())
                        .credentials(ACCESS_KEY, SECRET_KEY)
                        .build());
        ReflectionTestUtils.setField(misconfigured, "bucket", "bucket-that-was-never-created");
        ReflectionTestUtils.setField(misconfigured, "presignedUrlExpirySeconds", 600);

        assertThrows(
                StorageException.class,
                () -> misconfigured.upload(
                        "x", new ByteArrayInputStream("x".getBytes()), 1L, "text/plain"));
    }
}
