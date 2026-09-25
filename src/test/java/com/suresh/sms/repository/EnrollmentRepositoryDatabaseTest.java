package com.suresh.sms.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.entity.Course;
import com.suresh.sms.entity.Enrollment;
import com.suresh.sms.entity.Student;
import com.suresh.sms.support.AbstractIntegrationTest;

/**
 * Talks to a real, throwaway MySQL database (via Testcontainers) to prove
 * the enrollment schema's constraints actually hold - not just that the
 * Java code checks them, but that the database itself would reject a
 * violation even if the check were ever bypassed.
 *
 * Every test runs inside a transaction that is rolled back afterwards, so
 * no test data is left behind.
 */
@SpringBootTest
@Transactional
class EnrollmentRepositoryDatabaseTest extends AbstractIntegrationTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private Student student(String email) {
        return studentRepository.saveAndFlush(
                new Student(null, "Test Student", email, "MCA", BigDecimal.valueOf(1000)));
    }

    private Course course(String code, int capacity) {
        return courseRepository.saveAndFlush(new Course(null, code, "Test Course", capacity));
    }

    @Test
    void databaseRejectsEnrollingTheSameStudentInTheSameCourseTwice() {

        Student s = student("dup-enroll@example.com");
        Course c = course("DUP101", 30);

        enrollmentRepository.saveAndFlush(new Enrollment(s, c));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> enrollmentRepository.saveAndFlush(new Enrollment(s, c)));
    }

    @Test
    void databaseRejectsAnUnknownStudentOrCourse() {

        Course c = course("ORPHAN101", 30);

        Student ghost = new Student(999_999L, "Ghost", "ghost@example.com", "MCA",
                BigDecimal.valueOf(1000));

        assertThrows(
                Exception.class,
                () -> enrollmentRepository.saveAndFlush(new Enrollment(ghost, c)));
    }

    @Test
    void findByIdForUpdateReturnsTheSameCourseAsFindById() {

        Course c = course("LOCK101", 5);

        assertEquals(c.getId(), courseRepository.findByIdForUpdate(c.getId()).get().getId());
        assertTrue(courseRepository.findByIdForUpdate(999_999L).isEmpty());
    }

    @Test
    void countByCourseReflectsActualEnrollments() {

        Student s1 = student("count1@example.com");
        Student s2 = student("count2@example.com");
        Course c = course("COUNT101", 30);

        assertEquals(0, enrollmentRepository.countByCourse_Id(c.getId()));

        enrollmentRepository.saveAndFlush(new Enrollment(s1, c));
        enrollmentRepository.saveAndFlush(new Enrollment(s2, c));

        assertEquals(2, enrollmentRepository.countByCourse_Id(c.getId()));
    }

    @Test
    void existsQueriesWork() {

        Student s = student("exists-enroll@example.com");
        Course c = course("EXISTS101", 30);

        assertFalse(enrollmentRepository.existsByStudent_IdAndCourse_Id(s.getId(), c.getId()));
        assertFalse(enrollmentRepository.existsByCourse_Id(c.getId()));

        enrollmentRepository.saveAndFlush(new Enrollment(s, c));

        assertTrue(enrollmentRepository.existsByStudent_IdAndCourse_Id(s.getId(), c.getId()));
        assertTrue(enrollmentRepository.existsByCourse_Id(c.getId()));
    }
}
