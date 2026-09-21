package com.suresh.sms.exception;

/** Thrown when a student e-mail is already used by another student. Mapped to HTTP 409. */
public class DuplicateStudentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateStudentException(String message) {
        super(message);
    }
}
