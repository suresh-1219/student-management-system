-- V4: adds Course and Enrollment as new, additive tables.
--
-- The existing students.course column (free text, e.g. "MCA") is left
-- untouched for backward compatibility with existing data, tests and
-- endpoints. Enrollment is a separate concept: a student can additionally
-- be enrolled in named Course records that have a capacity limit.

CREATE TABLE courses (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(255) NOT NULL,
    title      VARCHAR(255) NOT NULL,
    capacity   INT          NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_courses_code UNIQUE (code)
);

CREATE TABLE enrollments (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    student_id  BIGINT      NOT NULL,
    course_id   BIGINT      NOT NULL,
    enrolled_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_enrollments_student_course UNIQUE (student_id, course_id),
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_enrollments_course  FOREIGN KEY (course_id)  REFERENCES courses (id)
);
