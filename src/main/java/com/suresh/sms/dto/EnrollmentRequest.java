package com.suresh.sms.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(

        @NotNull(message = "Student id is required")
        Long studentId,

        @NotNull(message = "Course id is required")
        Long courseId
) {
}
