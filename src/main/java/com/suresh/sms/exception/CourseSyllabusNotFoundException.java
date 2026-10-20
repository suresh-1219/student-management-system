package com.suresh.sms.exception;

/**
 * Thrown when a syllabus is requested or deleted for a course that doesn't
 * have one uploaded. Mapped to HTTP 404, same as the other *NotFound
 * exceptions.
 */
public class CourseSyllabusNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CourseSyllabusNotFoundException(String message) {
        super(message);
    }
}
