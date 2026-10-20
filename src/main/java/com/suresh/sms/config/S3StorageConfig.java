package com.suresh.sms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

/**
 * Builds the {@link MinioClient} used by {@code StorageService} to talk to
 * course-syllabus object storage.
 *
 * <p>Despite its name, {@code io.minio:minio} (the Java library this class
 * builds a client from) is a generic S3-protocol client, not something
 * tied to running the MinIO server specifically - it works against any
 * S3-compatible endpoint. This project points it at a
 * <a href="https://github.com/seaweedfs/seaweedfs">SeaweedFS</a> S3
 * gateway (see docker-compose.yml), not MinIO itself: MinIO discontinued
 * free container images for its open-source edition and the final free
 * release has an unpatched critical CVE, so it's no longer a sound choice
 * here even though it gave the feature its name back in Drop G2.
 *
 * <p>Bucket creation is handled separately, in
 * {@code StorageService#ensureBucketExists} on startup - this class only
 * wires up the client itself, the same way {@code RabbitMQConfig} only
 * wires up the broker connectivity, not the queues it uses.
 */
@Configuration
public class S3StorageConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${app.s3.endpoint}") String endpoint,
            @Value("${app.s3.access-key}") String accessKey,
            @Value("${app.s3.secret-key}") String secretKey) {

        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
