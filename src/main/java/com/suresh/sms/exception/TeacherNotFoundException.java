package com.suresh.sms.exception;

/** Thrown when a teacher id does not exist. Mapped to HTTP 404. */
public class TeacherNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TeacherNotFoundException(String message) {
        super(message);
    }
}
