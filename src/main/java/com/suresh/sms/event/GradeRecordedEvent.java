package com.suresh.sms.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published to RabbitMQ (see {@code RabbitMQConfig}) whenever
 * {@code GradeService.recordGrade} saves a grade, whether that is the first
 * time it was recorded or a correction. Serialized to JSON on the wire by
 * {@code Jackson2JsonMessageConverter}, so this and {@link
 * AttendanceRecordedEvent} are the only two payload types {@code
 * NotificationListener} needs to know how to read.
 */
public record GradeRecordedEvent(
        Long gradeId,
        Long enrollmentId,
        Long studentId,
        String studentName,
        Long courseId,
        String courseTitle,
        BigDecimal score,
        LocalDateTime recordedAt
) {
}
