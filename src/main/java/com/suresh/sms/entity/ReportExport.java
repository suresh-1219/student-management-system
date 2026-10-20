package com.suresh.sms.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One row per CSV report export request (Drop G3). Tracks the request
 * through its lifecycle - {@code PENDING} when accepted, {@code RUNNING}
 * once the batch job starts, then {@code COMPLETED} (with the object-storage
 * key the CSV was uploaded to) or {@code FAILED} (with {@code
 * errorMessage}) - so a client that triggered an export can poll for it
 * instead of holding a request open for however long the export takes.
 *
 * <p>Deliberately NOT one of the {@code BATCH_*} tables Spring Batch itself
 * owns (see V9__report_exports.sql): those record job-execution plumbing
 * (step counts, restart state, ...) that this app's own clients have no
 * business reading. This table is the public-facing summary a {@code
 * ReportExportDTO} is built from; {@code ReportExportJobListener} is what
 * keeps the two in sync.
 */
@Entity
@Table(name = "report_exports")
@EntityListeners(AuditingEntityListener.class)
public class ReportExport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 30)
    private ReportExportType exportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportExportStatus status;

    // Set together, by ReportExportJobListener, once the CSV has actually
    // been uploaded - mirrors Course's syllabus_object_key/
    // syllabus_original_filename/syllabus_size_bytes trio.
    @Column(name = "object_key")
    private String objectKey;

    @Column(name = "filename")
    private String filename;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public ReportExport() {
    }

    public ReportExport(ReportExportType exportType, ReportExportStatus status, String requestedBy) {
        this.exportType = exportType;
        this.status = status;
        this.requestedBy = requestedBy;
    }

    public boolean isCompleted() {
        return status == ReportExportStatus.COMPLETED;
    }

    public Long getId() {
        return id;
    }

    public ReportExportType getExportType() {
        return exportType;
    }

    public ReportExportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportExportStatus status) {
        this.status = status;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
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

    public String getRequestedBy() {
        return requestedBy;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
