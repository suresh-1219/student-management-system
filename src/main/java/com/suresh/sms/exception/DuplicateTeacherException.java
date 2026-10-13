package com.suresh.sms.exception;

/** Thrown when a teacher email is already used by another teacher. Mapped to HTTP 409. */
public class DuplicateTeacherException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateTeacherException(String message) {
        super(message);
    }
}
