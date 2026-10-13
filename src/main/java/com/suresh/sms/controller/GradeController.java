package com.suresh.sms.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.ApiResponse;
import com.suresh.sms.dto.GradeRequest;
import com.suresh.sms.dto.GradeResponseDTO;
import com.suresh.sms.service.GradeService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/grades")
@SecurityRequirement(name = "Bearer Authentication")
public class GradeController {

    @Autowired
    private GradeService service;


    // RECORD (OR CORRECT) A GRADE

    @PostMapping
    public ResponseEntity<ApiResponse<GradeResponseDTO>> recordGrade(
            @Valid @RequestBody GradeRequest request) {

        GradeResponseDTO grade = service.recordGrade(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Grade recorded successfully", grade));
    }


    // GET THE GRADE FOR ONE ENROLLMENT (null data if none recorded yet)

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<ApiResponse<GradeResponseDTO>> getGradeForEnrollment(
            @PathVariable Long enrollmentId) {

        GradeResponseDTO grade = service.getGradeForEnrollment(enrollmentId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Grade retrieved successfully", grade));
    }


    // LIST A STUDENT'S GRADES ACROSS ALL THEIR COURSES

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<GradeResponseDTO>>> getGradesForStudent(
            @PathVariable Long studentId) {

        List<GradeResponseDTO> grades = service.getGradesForStudent(studentId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Grades retrieved successfully", grades));
    }
}
