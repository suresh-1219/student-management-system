package com.suresh.sms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record GradeRequest(

        @NotNull(message = "Enrollment id is required")
        Long enrollmentId,

        @NotNull(message = "Score is required")
        @DecimalMin(value = "0.0", message = "Score cannot be negative")
        @DecimalMax(value = "100.0", message = "Score cannot exceed 100")
        @Digits(integer = 3, fraction = 2, message = "Score may have at most 2 decimal places")
        BigDecimal score
) {
}
