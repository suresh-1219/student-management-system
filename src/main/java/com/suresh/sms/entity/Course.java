package com.suresh.sms.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "courses")
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Course code is required")
    private String code;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than zero")
    private Integer capacity;

    // Nullable on purpose: a course can exist, and students can enroll in
    // it, before any teacher is assigned. Assigned via a dedicated endpoint
    // (see CourseController#assignTeacher), not the general update, so it
    // isn't accidentally cleared by an unrelated PUT /courses/{id}.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    // The syllabus file itself lives in S3-compatible object storage (see
    // StorageService), not here - these four columns only record where it
    // is and what it originally was. All null together when no syllabus
    // has been uploaded.
    // Set as a group by CourseService#uploadSyllabus / cleared together by
    // #deleteSyllabus, never edited individually.
    @Column(name = "syllabus_object_key")
    private String syllabusObjectKey;

    @Column(name = "syllabus_original_filename")
    private String syllabusOriginalFilename;

    @Column(name = "syllabus_content_type")
    private String syllabusContentType;

    @Column(name = "syllabus_size_bytes")
    private Long syllabusSizeBytes;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Course() {
    }

    public Course(Long id, String code, String title, Integer capacity) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public String getSyllabusObjectKey() {
        return syllabusObjectKey;
    }

    public void setSyllabusObjectKey(String syllabusObjectKey) {
        this.syllabusObjectKey = syllabusObjectKey;
    }

    public String getSyllabusOriginalFilename() {
        return syllabusOriginalFilename;
    }

    public void setSyllabusOriginalFilename(String syllabusOriginalFilename) {
        this.syllabusOriginalFilename = syllabusOriginalFilename;
    }

    public String getSyllabusContentType() {
        return syllabusContentType;
    }

    public void setSyllabusContentType(String syllabusContentType) {
        this.syllabusContentType = syllabusContentType;
    }

    public Long getSyllabusSizeBytes() {
        return syllabusSizeBytes;
    }

    public void setSyllabusSizeBytes(Long syllabusSizeBytes) {
        this.syllabusSizeBytes = syllabusSizeBytes;
    }

    public boolean hasSyllabus() {
        return syllabusObjectKey != null;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
