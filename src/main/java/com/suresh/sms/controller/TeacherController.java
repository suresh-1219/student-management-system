package com.suresh.sms.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.ApiResponse;
import com.suresh.sms.dto.TeacherDTO;
import com.suresh.sms.service.TeacherService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/teachers")
@SecurityRequirement(name = "Bearer Authentication")
public class TeacherController {

    @Autowired
    private TeacherService service;


    // CREATE TEACHER

    @PostMapping
    public ResponseEntity<ApiResponse<TeacherDTO>> saveTeacher(
            @Valid @RequestBody TeacherDTO dto) {

        TeacherDTO saved = service.saveTeacher(dto);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Teacher created successfully", saved));
    }


    // GET ALL TEACHERS

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeacherDTO>>> getAllTeachers() {

        List<TeacherDTO> teachers = service.getAllTeachers();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Teachers retrieved successfully", teachers));
    }


    // GET TEACHER BY ID

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherDTO>> getTeacherById(
            @PathVariable Long id) {

        TeacherDTO teacher = service.getTeacherById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Teacher retrieved successfully", teacher));
    }


    // UPDATE TEACHER

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherDTO>> updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody TeacherDTO dto) {

        TeacherDTO updated = service.updateTeacher(id, dto);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Teacher updated successfully", updated));
    }


    // DELETE TEACHER

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTeacher(
            @PathVariable Long id) {

        service.deleteTeacher(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Teacher deleted successfully", null));
    }
}
