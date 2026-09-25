package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suresh.sms.dto.EnrollmentRequest;
import com.suresh.sms.dto.EnrollmentResponseDTO;
import com.suresh.sms.entity.Course;
import com.suresh.sms.entity.Enrollment;
import com.suresh.sms.entity.Student;
import com.suresh.sms.exception.CourseCapacityExceededException;
import com.suresh.sms.exception.CourseNotFoundException;
import com.suresh.sms.exception.DuplicateEnrollmentException;
import com.suresh.sms.exception.EnrollmentNotFoundException;
import com.suresh.sms.exception.StudentNotFoundException;
import com.suresh.sms.repository.CourseRepository;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository repository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Student student(Long id) {
        Student s = new Student();
        s.setId(id);
        s.setName("Suresh");
        s.setEmail("suresh@gmail.com");
        s.setCourse("MCA");
        return s;
    }

    private Course course(Long id, int capacity) {
        return new Course(id, "CS101", "Intro to CS", capacity);
    }


    // ENROLL

    @Test
    void testEnrollSucceeds() {

        EnrollmentRequest request = new EnrollmentRequest(1L, 2L);
        Student student = student(1L);
        Course course = course(2L, 30);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(course));
        when(repository.existsByStudent_IdAndCourse_Id(1L, 2L)).thenReturn(false);
        when(repository.countByCourse_Id(2L)).thenReturn(5L);
        when(repository.save(any(Enrollment.class)))
                .thenAnswer(invocation -> {
                    Enrollment e = invocation.getArgument(0);
                    e.setId(10L);
                    return e;
                });

        EnrollmentResponseDTO result = enrollmentService.enroll(request);

        assertEquals(10L, result.getId());
        assertEquals(1L, result.getStudentId());
        assertEquals(2L, result.getCourseId());
        assertEquals("CS101", result.getCourseCode());
    }

    @Test
    void testEnrollRejectsUnknownStudent() {

        EnrollmentRequest request = new EnrollmentRequest(99L, 2L);

        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                StudentNotFoundException.class,
                () -> enrollmentService.enroll(request));

        verify(repository, never()).save(any(Enrollment.class));
    }

    @Test
    void testEnrollRejectsUnknownCourse() {

        EnrollmentRequest request = new EnrollmentRequest(1L, 99L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L)));
        when(courseRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(
                CourseNotFoundException.class,
                () -> enrollmentService.enroll(request));

        verify(repository, never()).save(any(Enrollment.class));
    }

    @Test
    void testEnrollRejectsDuplicateEnrollment() {

        EnrollmentRequest request = new EnrollmentRequest(1L, 2L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L)));
        when(courseRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(course(2L, 30)));
        when(repository.existsByStudent_IdAndCourse_Id(1L, 2L)).thenReturn(true);

        assertThrows(
                DuplicateEnrollmentException.class,
                () -> enrollmentService.enroll(request));

        verify(repository, never()).save(any(Enrollment.class));
    }

    @Test
    void testEnrollRejectsWhenCourseIsFull() {

        EnrollmentRequest request = new EnrollmentRequest(1L, 2L);
        Course fullCourse = course(2L, 2);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L)));
        when(courseRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(fullCourse));
        when(repository.existsByStudent_IdAndCourse_Id(1L, 2L)).thenReturn(false);
        when(repository.countByCourse_Id(2L)).thenReturn(2L);

        CourseCapacityExceededException ex = assertThrows(
                CourseCapacityExceededException.class,
                () -> enrollmentService.enroll(request));

        assertEquals("This course is full (capacity: 2)", ex.getMessage());
        verify(repository, never()).save(any(Enrollment.class));
    }


    // UNENROLL

    @Test
    void testUnenrollSucceeds() {

        Enrollment existing = new Enrollment(student(1L), course(2L, 30));

        when(repository.findById(10L)).thenReturn(Optional.of(existing));

        enrollmentService.unenroll(10L);

        verify(repository).delete(existing);
    }

    @Test
    void testUnenrollNotFound() {

        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(
                EnrollmentNotFoundException.class,
                () -> enrollmentService.unenroll(10L));
    }


    // LIST

    @Test
    void testGetEnrollmentsForStudentNotFound() {

        when(studentRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                StudentNotFoundException.class,
                () -> enrollmentService.getEnrollmentsForStudent(99L));
    }

    @Test
    void testGetEnrollmentsForCourseNotFound() {

        when(courseRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                CourseNotFoundException.class,
                () -> enrollmentService.getEnrollmentsForCourse(99L));
    }

    @Test
    void testGetEnrollmentsForStudentReturnsList() {

        Enrollment e = new Enrollment(student(1L), course(2L, 30));
        e.setId(10L);

        when(studentRepository.existsById(1L)).thenReturn(true);
        when(repository.findByStudent_Id(1L)).thenReturn(List.of(e));

        List<EnrollmentResponseDTO> result = enrollmentService.getEnrollmentsForStudent(1L);

        assertEquals(1, result.size());
        assertEquals("CS101", result.get(0).getCourseCode());
    }
}
