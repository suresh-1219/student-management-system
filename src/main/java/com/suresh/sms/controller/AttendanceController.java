package com.suresh.sms.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.ApiResponse;
import com.suresh.sms.dto.AttendanceRequest;
import com.suresh.sms.dto.AttendanceResponseDTO;
import com.suresh.sms.service.AttendanceService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/attendance")
@SecurityRequirement(name = "Bearer Authentication")
public class AttendanceController {

    @Autowired
    private AttendanceService service;


    // RECORD (OR CORRECT) ONE DAY'S ATTENDANCE FOR ONE ENROLLMENT

    @PostMapping
    public ResponseEntity<ApiResponse<AttendanceResponseDTO>> recordAttendance(
            @Valid @RequestBody AttendanceRequest request) {

        AttendanceResponseDTO record = service.recordAttendance(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Attendance recorded successfully", record));
    }


    // LIST ONE ENROLLMENT'S ATTENDANCE HISTORY

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<ApiResponse<List<AttendanceResponseDTO>>> getAttendanceForEnrollment(
            @PathVariable Long enrollmentId) {

        List<AttendanceResponseDTO> records =
                service.getAttendanceForEnrollment(enrollmentId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Attendance retrieved successfully", records));
    }
}
