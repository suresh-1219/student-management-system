package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suresh.sms.dto.GradeRequest;
import com.suresh.sms.dto.GradeResponseDTO;
import com.suresh.sms.entity.Course;
import com.suresh.sms.entity.Enrollment;
import com.suresh.sms.entity.Grade;
import com.suresh.sms.entity.Student;
import com.suresh.sms.exception.EnrollmentNotFoundException;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.GradeRepository;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository repository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private GradeService gradeService;

    private Enrollment enrollment(Long id) {

        Student student = new Student(10L, "Asha", "asha@mail.com", "CS101", BigDecimal.ZERO);
        Course course = new Course(20L, "CS101", "Intro to CS", 30);

        Enrollment enrollment = new Enrollment(student, course);
        enrollment.setId(id);
        return enrollment;
    }


    // RECORD GRADE (UPSERT)

    @Test
    void testRecordGradeEnrollmentNotFound() {

        GradeRequest request = new GradeRequest(99L, new BigDecimal("85.00"));

        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                EnrollmentNotFoundException.class,
                () -> gradeService.recordGrade(request));
    }

    @Test
    void testRecordGradeCreatesWhenNoneExists() {

        Enrollment enrollment = enrollment(1L);
        GradeRequest request = new GradeRequest(1L, new BigDecimal("85.00"));

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(repository.findByEnrollment_Id(1L)).thenReturn(Optional.empty());
        when(repository.save(any(Grade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GradeResponseDTO result = gradeService.recordGrade(request);

        assertEquals(1L, result.enrollmentId());
        assertEquals(10L, result.studentId());
        assertEquals("Asha", result.studentName());
        assertEquals(20L, result.courseId());
        assertEquals(new BigDecimal("85.00"), result.score());
    }

    @Test
    void testRecordGradeCorrectsExistingGradeInsteadOfCreatingASecondRow() {

        Enrollment enrollment = enrollment(1L);
        Grade existingGrade = new Grade(enrollment, new BigDecimal("70.00"));
        GradeRequest request = new GradeRequest(1L, new BigDecimal("92.50"));

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(repository.findByEnrollment_Id(1L)).thenReturn(Optional.of(existingGrade));

        ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        GradeResponseDTO result = gradeService.recordGrade(request);

        // The same Grade instance is reused and corrected, not a new one created.
        assertEquals(existingGrade, captor.getValue());
        assertEquals(new BigDecimal("92.50"), result.score());
    }


    // GET FOR ENROLLMENT

    @Test
    void testGetGradeForEnrollmentNotFoundThrows() {

        when(enrollmentRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                EnrollmentNotFoundException.class,
                () -> gradeService.getGradeForEnrollment(99L));
    }

    @Test
    void testGetGradeForEnrollmentReturnsNullWhenNotYetGraded() {

        when(enrollmentRepository.existsById(1L)).thenReturn(true);
        when(repository.findByEnrollment_Id(1L)).thenReturn(Optional.empty());

        GradeResponseDTO result = gradeService.getGradeForEnrollment(1L);

        assertNull(result);
    }


    // GET FOR STUDENT

    @Test
    void testGetGradesForStudent() {

        Enrollment enrollment = enrollment(1L);
        Grade grade = new Grade(enrollment, new BigDecimal("88.00"));

        when(repository.findByEnrollment_Student_Id(10L)).thenReturn(List.of(grade));

        List<GradeResponseDTO> result = gradeService.getGradesForStudent(10L);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("88.00"), result.get(0).score());
    }
}
