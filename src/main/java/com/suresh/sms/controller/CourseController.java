package com.suresh.sms.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.ApiResponse;
import com.suresh.sms.dto.CourseDTO;
import com.suresh.sms.service.CourseService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/courses")
@SecurityRequirement(name = "Bearer Authentication")
public class CourseController {

    @Autowired
    private CourseService service;


    // CREATE COURSE

    @PostMapping
    public ResponseEntity<ApiResponse<CourseDTO>> saveCourse(
            @Valid @RequestBody CourseDTO dto) {

        CourseDTO saved = service.saveCourse(dto);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Course created successfully", saved));
    }


    // GET ALL COURSES

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseDTO>>> getAllCourses() {

        List<CourseDTO> courses = service.getAllCourses();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Courses retrieved successfully", courses));
    }


    // GET COURSE BY ID

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDTO>> getCourseById(
            @PathVariable Long id) {

        CourseDTO course = service.getCourseById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Course retrieved successfully", course));
    }


    // UPDATE COURSE

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDTO>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDTO dto) {

        CourseDTO updated = service.updateCourse(id, dto);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Course updated successfully", updated));
    }


    // DELETE COURSE

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCourse(
            @PathVariable Long id) {

        service.deleteCourse(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Course deleted successfully", null));
    }
}
