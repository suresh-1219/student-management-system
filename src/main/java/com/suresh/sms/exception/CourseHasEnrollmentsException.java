package com.suresh.sms.exception;

/**
 * Thrown when trying to delete a course that still has students enrolled in
 * it. Mapped to HTTP 409 - the caller must unenroll every student first.
 */
public class CourseHasEnrollmentsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CourseHasEnrollmentsException(String message) {
        super(message);
    }
}
