package com.suresh.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CourseDTO {

    private Long id;

    @NotBlank(message = "Course code is required")
    private String code;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than zero")
    private Integer capacity;

    // Output-only: populated by CourseService from the course's assigned
    // teacher, never read from the request body. Assigning a teacher is a
    // dedicated action (PUT /courses/{id}/teacher, via AssignTeacherRequest),
    // not part of this DTO's own create/update, so a stray value sent here
    // is simply ignored rather than silently changing the assignment.
    private Long teacherId;
    private String teacherName;

    // Output-only, same reasoning as teacherId/teacherName above: populated
    // by CourseService from the course's syllabus columns, never read from
    // the request body. The object key itself is never exposed - only
    // CourseService/StorageService know it; the controller returns a
    // presigned URL instead (see CourseController#getSyllabus).
    private String syllabusFilename;
    private Long syllabusSizeBytes;

    public CourseDTO() {
    }

    public CourseDTO(Long id, String code, String title, Integer capacity) {
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

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getSyllabusFilename() {
        return syllabusFilename;
    }

    public void setSyllabusFilename(String syllabusFilename) {
        this.syllabusFilename = syllabusFilename;
    }

    public Long getSyllabusSizeBytes() {
        return syllabusSizeBytes;
    }

    public void setSyllabusSizeBytes(Long syllabusSizeBytes) {
        this.syllabusSizeBytes = syllabusSizeBytes;
    }
}
