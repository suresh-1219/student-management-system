package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.suresh.sms.exception.StorageException;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
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

        InputStream data = new ByteArrayInputStream("content".getBytes());

        storageService.upload("courses/1/abc", data, 7L, "application/pdf");

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void testUploadWrapsFailureInStorageException() throws Exception {

        InputStream data = new ByteArrayInputStream("content".getBytes());

        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(errorResponseException());

        assertThrows(
                StorageException.class,
                () -> storageService.upload("courses/1/abc", data, 7L, "application/pdf"));
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
