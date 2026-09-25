package com.suresh.sms.exception;

/** Thrown when a course has no remaining seats. Mapped to HTTP 409. */
public class CourseCapacityExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CourseCapacityExceededException(String message) {
        super(message);
    }
}
