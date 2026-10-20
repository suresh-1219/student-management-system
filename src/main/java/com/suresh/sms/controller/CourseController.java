package com.suresh.sms.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.suresh.sms.dto.ApiResponse;
import com.suresh.sms.dto.AssignTeacherRequest;
import com.suresh.sms.dto.CourseDTO;
import com.suresh.sms.dto.SyllabusDTO;
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


    // ASSIGN (OR, WITH A NULL teacherId, UNASSIGN) THE COURSE'S TEACHER

    @PutMapping("/{id}/teacher")
    public ResponseEntity<ApiResponse<CourseDTO>> assignTeacher(
            @PathVariable Long id,
            @RequestBody AssignTeacherRequest request) {

        CourseDTO updated = service.assignTeacher(id, request.teacherId());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Teacher assignment updated successfully", updated));
    }


    // DELETE COURSE

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCourse(
            @PathVariable Long id) {

        service.deleteCourse(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Course deleted successfully", null));
    }


    // SYLLABUS (file attachment; see StorageService)

    // ADMIN only - rides the existing POST /courses/** rule in
    // SecurityConfig, same as saveCourse above.
    @PostMapping("/{id}/syllabus")
    public ResponseEntity<ApiResponse<CourseDTO>> uploadSyllabus(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        CourseDTO updated = service.uploadSyllabus(id, file);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Syllabus uploaded successfully", updated));
    }

    // USER + ADMIN can read - rides the existing GET /courses/** rule.
    // Returns a short-lived presigned URL rather than streaming the file
    // itself, so the download happens directly against the S3-compatible object store.
    @GetMapping("/{id}/syllabus")
    public ResponseEntity<ApiResponse<SyllabusDTO>> getSyllabus(
            @PathVariable Long id) {

        SyllabusDTO syllabus = service.getSyllabusDownload(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Syllabus retrieved successfully", syllabus));
    }

    // ADMIN only - rides the existing DELETE /courses/** rule.
    @DeleteMapping("/{id}/syllabus")
    public ResponseEntity<ApiResponse<String>> deleteSyllabus(
            @PathVariable Long id) {

        service.deleteSyllabus(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Syllabus deleted successfully", null));
    }
}
