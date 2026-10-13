package com.suresh.sms.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GradeResponseDTO(
        Long id,
        Long enrollmentId,
        Long studentId,
        String studentName,
        Long courseId,
        String courseTitle,
        BigDecimal score,
        LocalDateTime gradedAt
) {
}
