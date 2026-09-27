package com.suresh.sms.exception;

/** Thrown when a refresh token is unknown, expired, or already used/revoked. Mapped to HTTP 401. */
public class InvalidRefreshTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
