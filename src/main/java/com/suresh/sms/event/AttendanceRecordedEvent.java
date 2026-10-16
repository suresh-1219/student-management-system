package com.suresh.sms.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.suresh.sms.entity.AttendanceStatus;

/**
 * Published to RabbitMQ (see {@code RabbitMQConfig}) whenever {@code
 * AttendanceService.recordAttendance} saves an attendance entry, whether
 * that is the first time that day was recorded or a correction.
 */
public record AttendanceRecordedEvent(
        Long attendanceId,
        Long enrollmentId,
        Long studentId,
        String studentName,
        Long courseId,
        String courseTitle,
        LocalDate date,
        AttendanceStatus status,
        LocalDateTime recordedAt
) {
}
