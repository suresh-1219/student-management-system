package com.suresh.sms.exception;

/** Thrown when deleting a teacher who is still assigned to at least one course. Mapped to HTTP 409. */
public class TeacherHasCoursesException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TeacherHasCoursesException(String message) {
        super(message);
    }
}
