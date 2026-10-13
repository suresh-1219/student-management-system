package com.suresh.sms.dto;

import java.util.List;

public record StudentReportDTO(
        Long studentId,
        String studentName,
        List<CourseReportEntry> courses
) {
}
