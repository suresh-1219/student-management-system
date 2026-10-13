package com.suresh.sms.dto;

import java.time.LocalDate;

import com.suresh.sms.entity.AttendanceStatus;

public record AttendanceResponseDTO(
        Long id,
        Long enrollmentId,
        Long studentId,
        Long courseId,
        String courseTitle,
        LocalDate date,
        AttendanceStatus status
) {
}
