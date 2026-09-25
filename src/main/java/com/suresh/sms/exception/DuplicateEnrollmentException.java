package com.suresh.sms.exception;

/** Thrown when a student is already enrolled in a course. Mapped to HTTP 409. */
public class DuplicateEnrollmentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateEnrollmentException(String message) {
        super(message);
    }
}
