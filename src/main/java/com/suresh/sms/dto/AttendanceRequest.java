package com.suresh.sms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import com.suresh.sms.entity.AttendanceStatus;

public record AttendanceRequest(

        @NotNull(message = "Enrollment id is required")
        Long enrollmentId,

        @NotNull(message = "Date is required")
        @PastOrPresent(message = "Attendance date cannot be in the future")
        LocalDate date,

        @NotNull(message = "Status is required")
        AttendanceStatus status
) {
}
