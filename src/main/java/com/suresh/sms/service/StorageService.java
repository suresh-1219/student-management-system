package com.suresh.sms.service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.suresh.sms.exception.StorageException;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;

import jakarta.annotation.PostConstruct;

/**
 * Thin wrapper around an S3-compatible object store: upload, delete, and a
 * short-lived presigned download URL. Generic on purpose (object key in,
 * object key out) so it isn't tied to courses/syllabi specifically - {@code
 * CourseService} is the only caller today, but a later feature (Drop G3's
 * CSV report export, for instance) can reuse this unchanged.
 *
 * <p>The backing server is <a href="https://github.com/seaweedfs/seaweedfs">
 * SeaweedFS</a>'s S3 gateway (see docker-compose.yml), reached through the
 * {@code io.minio:minio} client library - that library is a generic
 * S3-protocol client despite its name, not something that requires running
 * MinIO itself. (MinIO's own free container images were pulled from both
 * Docker Hub and quay.io after this feature was first built against them,
 * and the last free release has an unpatched critical CVE, so this project
 * no longer runs MinIO's server at all - only its client library remains,
 * now pointed elsewhere.)
 *
 * <p>Every checked {@link MinioException} (and the handful of other
 * checked exceptions the client throws) is wrapped in an unchecked
 * {@link StorageException}. Unlike {@code CacheErrorHandler} for Redis or
 * {@code EventPublisher} for RabbitMQ, failures here are NOT swallowed:
 * object storage is the actual point of a syllabus upload/download/delete
 * request, so a failure here must fail that request.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @Autowired
    private MinioClient minioClient;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.presigned-url-expiry-seconds:600}")
    private int presignedUrlExpirySeconds;

    /**
     * Creates the bucket on startup if it doesn't already exist, so a fresh
     * object store (a new docker-compose volume, a first-time local run)
     * works without a manual setup step. Idempotent - safe to run on every
     * restart.
     */
    @PostConstruct
    public void ensureBucketExists() {

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());

            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created object storage bucket '{}'", bucket);
            }
        } catch (Exception ex) {
            // Deliberately not fatal: the object store being unreachable at
            // startup (for example a plain `mvn spring-boot:run` with no
            // SeaweedFS running) shouldn't stop the whole app from coming
            // up - only the syllabus upload/download/delete endpoints will
            // fail, with a clear 502, until it's reachable.
            log.warn("Could not confirm/create object storage bucket '{}' on startup "
                    + "(will retry on first use): {}", bucket, ex.getMessage());
        }
    }

    public void upload(
            String objectKey,
            InputStream data,
            long size,
            String contentType) {

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(data, size, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception ex) {
            throw new StorageException(
                    "Failed to upload object '" + objectKey + "'", ex);
        }
    }

    public void delete(String objectKey) {

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
        } catch (Exception ex) {
            throw new StorageException(
                    "Failed to delete object '" + objectKey + "'", ex);
        }
    }

    public int getPresignedUrlExpirySeconds() {
        return presignedUrlExpirySeconds;
    }

    /**
     * A time-limited URL the client can download the object from directly,
     * without the request passing back through this app. Expires after
     * {@code app.s3.presigned-url-expiry-seconds} (default 10 minutes).
     */
    public String presignedDownloadUrl(String objectKey) {

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(presignedUrlExpirySeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception ex) {
            throw new StorageException(
                    "Failed to generate a download URL for object '" + objectKey + "'", ex);
        }
    }
}
