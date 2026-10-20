package com.suresh.sms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suresh.sms.dto.ApiResponse;
import com.suresh.sms.dto.ReportDownloadDTO;
import com.suresh.sms.dto.ReportExportDTO;
import com.suresh.sms.service.ReportExportService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * CSV report exports (Drop G3). ADMIN-only throughout, unlike most other
 * {@code GET}s in this API (see SecurityConfig): a student export is a bulk
 * dump of everyone's name/email/fee, not a single record a USER has a
 * legitimate reason to read.
 */
@RestController
@RequestMapping("/reports")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    @Autowired
    private ReportExportService service;

    // Kicks off the export and returns immediately (status PENDING) - the
    // batch job itself runs asynchronously (see BatchConfig). The client
    // polls GET /reports/exports/{id} for progress.
    //
    // Reads SecurityContextHolder directly rather than taking an
    // Authentication method parameter: the latter is resolved from the
    // servlet request's principal, which only Spring Security's own
    // request-wrapping filter populates - reading the (thread-bound)
    // SecurityContext instead works the same way in production and in a
    // MockMvc test that disables the filter chain.
    @PostMapping("/students/export")
    public ResponseEntity<ApiResponse<ReportExportDTO>> exportStudents() {

        String requestedBy = SecurityContextHolder.getContext().getAuthentication().getName();

        ReportExportDTO export = service.requestStudentExport(requestedBy);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(true, "Export started", export));
    }

    @GetMapping("/exports/{id}")
    public ResponseEntity<ApiResponse<ReportExportDTO>> getExport(
            @PathVariable Long id) {

        ReportExportDTO export = service.getExport(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Export status retrieved successfully", export));
    }

    // A short-lived presigned URL, same shape as CourseController#getSyllabus
    // - the download happens directly against the S3-compatible object
    // store, not through this app.
    @GetMapping("/exports/{id}/download")
    public ResponseEntity<ApiResponse<ReportDownloadDTO>> getExportDownload(
            @PathVariable Long id) {

        ReportDownloadDTO download = service.getExportDownload(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Download link generated successfully", download));
    }
}
