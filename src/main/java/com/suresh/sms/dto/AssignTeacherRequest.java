package com.suresh.sms.dto;

/**
 * Body for {@code PUT /courses/{id}/teacher}. {@code teacherId} may be
 * {@code null} to unassign the course's current teacher.
 */
public record AssignTeacherRequest(Long teacherId) {
}
