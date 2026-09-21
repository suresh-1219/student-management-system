package com.suresh.sms.exception;

/**
 * Thrown when a request is well-formed JSON but asks for something the API
 * does not allow (for example sorting by an unknown field or requesting a
 * page size above the limit). Mapped to HTTP 400.
 */
public class InvalidRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }
}
