package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.suresh.sms.exception.StorageException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageService, "bucket", "course-syllabi");
        ReflectionTestUtils.setField(storageService, "presignedUrlExpirySeconds", 600);

        // Real (non-mocked) instances - resilience4j's own classes are
        // plain enough to construct directly, no Spring context needed -
        // configured so every test below this point gets exactly ONE
        // attempt, same as before this drop added retry/circuit-breaker
        // wiring. Tests further down that specifically exercise retry or
        // circuit-breaker behavior install their own instances instead.
        ReflectionTestUtils.setField(storageService, "uploadRetry",
                Retry.of("test-upload", RetryConfig.custom().maxAttempts(1).build()));
        ReflectionTestUtils.setField(storageService, "uploadCircuitBreaker",
                CircuitBreaker.ofDefaults("test-upload"));
    }


    // BUCKET BOOTSTRAP

    @Test
    void testEnsureBucketExistsCreatesBucketWhenMissing() throws Exception {

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        storageService.ensureBucketExists();

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void testEnsureBucketExistsSkipsCreationWhenAlreadyPresent() throws Exception {

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        storageService.ensureBucketExists();

        verify(minioClient, org.mockito.Mockito.never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void testEnsureBucketExistsNeverThrowsWhenObjectStoreIsUnreachable() throws Exception {

        // Startup must not fail just because the object store isn't
        // running yet (see the Javadoc on
        // StorageService#ensureBucketExists) - it only logs a warning and
        // lets the first real call fail loudly instead.
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("connection refused"));

        storageService.ensureBucketExists();
    }


    // UPLOAD

    @Test
    void testUploadSucceeds() throws Exception {

        storageService.upload(
                "courses/1/abc", () -> new ByteArrayInputStream("content".getBytes()),
                7L, "application/pdf");

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void testUploadWrapsFailureInStorageException() throws Exception {

        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(errorResponseException());

        assertThrows(
                StorageException.class,
                () -> storageService.upload(
                        "courses/1/abc", () -> new ByteArrayInputStream("content".getBytes()),
                        7L, "application/pdf"));
    }

    @Test
    void testUploadRetriesOnTransientFailureThenSucceeds() throws Exception {

        // Its own Retry instance, not the one-attempt default from setUp():
        // this is the one test that actually exercises the "ride out a
        // transient blip" behavior the class Javadoc describes. A near-zero
        // wait keeps the test fast - the point being tested is the retry
        // count, not the backoff timing.
        ReflectionTestUtils.setField(storageService, "uploadRetry",
                Retry.of("test-upload-retry",
                        RetryConfig.custom().maxAttempts(3).waitDuration(Duration.ofMillis(5)).build()));

        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(errorResponseException())
                .thenThrow(errorResponseException())
                .thenReturn(org.mockito.Mockito.mock(ObjectWriteResponse.class));

        storageService.upload(
                "courses/1/abc", () -> new ByteArrayInputStream("content".getBytes()),
                7L, "application/pdf");

        // Two failed attempts, then a third that succeeds - proves a
        // transient failure doesn't fail the request, not just that
        // *a* call eventually went through.
        verify(minioClient, times(3)).putObject(any(PutObjectArgs.class));
    }

    @Test
    void testUploadFailsFastWhenCircuitBreakerOpen() throws Exception {

        // Forced open rather than tripped by real failures: this test is
        // about what happens to a call once the circuit IS open, not about
        // how many failures it takes to get there (ResilienceConfig's
        // threshold is covered by inspection, not by re-deriving it here).
        CircuitBreaker openBreaker = CircuitBreaker.ofDefaults("test-upload-open");
        openBreaker.transitionToOpenState();
        ReflectionTestUtils.setField(storageService, "uploadCircuitBreaker", openBreaker);

        assertThrows(
                StorageException.class,
                () -> storageService.upload(
                        "courses/1/abc", () -> new ByteArrayInputStream("content".getBytes()),
                        7L, "application/pdf"));

        // Refused by the breaker before ever reaching the client - the
        // "fails fast" half of the retry/circuit-breaker pairing.
        verify(minioClient, org.mockito.Mockito.never()).putObject(any(PutObjectArgs.class));
    }


    // DELETE

    @Test
    void testDeleteSucceeds() throws Exception {

        storageService.delete("courses/1/abc");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void testDeleteWrapsFailureInStorageException() throws Exception {

        org.mockito.Mockito.doThrow(errorResponseException())
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThrows(
                StorageException.class,
                () -> storageService.delete("courses/1/abc"));
    }


    // PRESIGNED DOWNLOAD URL

    @Test
    void testPresignedDownloadUrlReturnsClientValue() throws Exception {

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://objectstore.local/course-syllabi/courses/1/abc?X-Amz-Expires=600");

        String url = storageService.presignedDownloadUrl("courses/1/abc");

        assertEquals(
                "https://objectstore.local/course-syllabi/courses/1/abc?X-Amz-Expires=600", url);
    }

    @Test
    void testPresignedDownloadUrlWrapsFailureInStorageException() throws Exception {

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(errorResponseException());

        assertThrows(
                StorageException.class,
                () -> storageService.presignedDownloadUrl("courses/1/abc"));
    }

    @Test
    void testGetPresignedUrlExpirySecondsReturnsConfiguredValue() {
        assertEquals(600, storageService.getPresignedUrlExpirySeconds());
    }

    // MinIO's own checked exception type is awkward to construct directly
    // (its constructor needs a parsed error-response object); a generic
    // RuntimeException is just as valid a stand-in here, since
    // StorageService treats every exception identically - it only needs
    // *something* to be thrown by the mock.
    private RuntimeException errorResponseException() {
        return new RuntimeException("MinIO error");
    }
}
