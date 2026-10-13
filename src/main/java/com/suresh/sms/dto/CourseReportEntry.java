package com.suresh.sms.dto;

import java.math.BigDecimal;

/**
 * One course's slice of a {@link StudentReportDTO}: the grade (if one has
 * been recorded) and an attendance summary for that course.
 *
 * <p>{@code grade} is {@code null} when no grade has been recorded yet -
 * that's a normal state, not an error. {@code attendancePercentage} is
 * {@code null} when there are zero attendance records yet, to distinguish
 * "no data" from "0% attendance" (a real, if alarming, data point).
 */
public record CourseReportEntry(
        Long enrollmentId,
        Long courseId,
        String courseCode,
        String courseTitle,
        BigDecimal grade,
        int attendanceTotalDays,
        int attendancePresentDays,
        BigDecimal attendancePercentage
) {
}
