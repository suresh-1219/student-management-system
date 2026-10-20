package com.suresh.sms.exception;

/**
 * Thrown when the S3-compatible object store itself fails (unreachable, bucket missing, a
 * malformed response, ...) - as opposed to the request simply being
 * invalid. Unlike Redis and RabbitMQ (which degrade gracefully; see
 * application.properties), object storage is the point of the request for
 * a syllabus upload/download/delete, so a failure here must fail the
 * request rather than being swallowed.
 *
 * <p>Mapped to HTTP 502 (Bad Gateway): the API call itself was fine, an
 * upstream dependency failed.
 */
public class StorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
