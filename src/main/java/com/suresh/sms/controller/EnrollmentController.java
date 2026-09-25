package com.suresh.sms.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.ApiResponse;
import com.suresh.sms.dto.EnrollmentRequest;
import com.suresh.sms.dto.EnrollmentResponseDTO;
import com.suresh.sms.service.EnrollmentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/enrollments")
@SecurityRequirement(name = "Bearer Authentication")
public class EnrollmentController {

    @Autowired
    private EnrollmentService service;


    // ENROLL A STUDENT IN A COURSE

    @PostMapping
    public ResponseEntity<ApiResponse<EnrollmentResponseDTO>> enroll(
            @Valid @RequestBody EnrollmentRequest request) {

        EnrollmentResponseDTO enrollment = service.enroll(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Student enrolled successfully", enrollment));
    }


    // UNENROLL (by the enrollment's own id)

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> unenroll(
            @PathVariable Long id) {

        service.unenroll(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Student unenrolled successfully", null));
    }


    // LIST A STUDENT'S ENROLLMENTS

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<EnrollmentResponseDTO>>> getEnrollmentsForStudent(
            @PathVariable Long studentId) {

        List<EnrollmentResponseDTO> enrollments =
                service.getEnrollmentsForStudent(studentId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Enrollments retrieved successfully", enrollments));
    }


    // LIST A COURSE'S ENROLLMENTS

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<EnrollmentResponseDTO>>> getEnrollmentsForCourse(
            @PathVariable Long courseId) {

        List<EnrollmentResponseDTO> enrollments =
                service.getEnrollmentsForCourse(courseId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Enrollments retrieved successfully", enrollments));
    }
}
