package com.suresh.sms.dto;

import java.time.LocalDateTime;

/**
 * Status of one report export request - what {@code POST /reports/students/
 * export} returns immediately (status {@code PENDING}), and what {@code GET
 * /reports/exports/{id}} returns on every subsequent poll. Does NOT carry a
 * download link itself - once {@code status} is {@code COMPLETED}, the
 * client calls {@code GET /reports/exports/{id}/download} for a (short-lived)
 * presigned URL, the same two-step shape {@code CourseDTO}/{@code
 * SyllabusDTO} already use for syllabi.
 */
public class ReportExportDTO {

    private Long id;
    private String exportType;
    private String status;
    private String filename;
    private Long sizeBytes;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public ReportExportDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
