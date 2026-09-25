package com.suresh.sms.exception;

/** Thrown when a course code is already used by another course. Mapped to HTTP 409. */
public class DuplicateCourseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateCourseException(String message) {
        super(message);
    }
}
