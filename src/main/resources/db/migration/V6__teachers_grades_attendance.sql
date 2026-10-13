-- V6: adds Teacher, Grade and Attendance as new, additive tables, plus an
-- optional teacher assignment on Course.
--
-- A course's teacher is nullable on purpose: a course can exist (and
-- students can enroll in it) before a teacher is assigned.
--
-- Grade is a one-to-one with Enrollment (uk_grades_enrollment): a student
-- gets one grade per course they are enrolled in, which can be corrected by
-- recording it again (see GradeService - it upserts, not inserts-only).
--
-- Attendance is one row per enrollment per calendar day
-- (uk_attendance_enrollment_date), also upsertable for the same reason: a
-- day's status can be corrected without creating a duplicate row.

CREATE TABLE teachers (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_teachers_email UNIQUE (email)
);

ALTER TABLE courses
    ADD COLUMN teacher_id BIGINT NULL,
    ADD CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id);

CREATE TABLE grades (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT        NOT NULL,
    score         DECIMAL(5,2)  NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    version       BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_grades_enrollment UNIQUE (enrollment_id),
    CONSTRAINT fk_grades_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
    -- Defense in depth: @DecimalMin/@DecimalMax on GradeRequest already
    -- reject this, but the database enforces it too in case a row is ever
    -- written some other way.
    CONSTRAINT chk_grades_score CHECK (score >= 0 AND score <= 100)
);

CREATE TABLE attendance_records (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    enrollment_id    BIGINT       NOT NULL,
    attendance_date  DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendance_enrollment_date UNIQUE (enrollment_id, attendance_date),
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
);
