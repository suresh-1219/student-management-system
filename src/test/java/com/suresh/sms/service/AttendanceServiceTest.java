package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suresh.sms.dto.AttendanceRequest;
import com.suresh.sms.dto.AttendanceResponseDTO;
import com.suresh.sms.entity.AttendanceRecord;
import com.suresh.sms.entity.AttendanceStatus;
import com.suresh.sms.entity.Course;
import com.suresh.sms.entity.Enrollment;
import com.suresh.sms.entity.Student;
import com.suresh.sms.exception.EnrollmentNotFoundException;
import com.suresh.sms.repository.AttendanceRecordRepository;
import com.suresh.sms.repository.EnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRecordRepository repository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private Enrollment enrollment(Long id) {

        Student student = new Student(10L, "Asha", "asha@mail.com", "CS101", BigDecimal.ZERO);
        Course course = new Course(20L, "CS101", "Intro to CS", 30);

        Enrollment enrollment = new Enrollment(student, course);
        enrollment.setId(id);
        return enrollment;
    }


    // RECORD ATTENDANCE (UPSERT)

    @Test
    void testRecordAttendanceEnrollmentNotFound() {

        AttendanceRequest request =
                new AttendanceRequest(99L, LocalDate.now(), AttendanceStatus.PRESENT);

        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                EnrollmentNotFoundException.class,
                () -> attendanceService.recordAttendance(request));
    }

    @Test
    void testRecordAttendanceCreatesWhenNoneExistsForThatDay() {

        Enrollment enrollment = enrollment(1L);
        LocalDate date = LocalDate.now();
        AttendanceRequest request = new AttendanceRequest(1L, date, AttendanceStatus.PRESENT);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(repository.findByEnrollment_IdAndAttendanceDate(1L, date)).thenReturn(Optional.empty());
        when(repository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceResponseDTO result = attendanceService.recordAttendance(request);

        assertEquals(1L, result.enrollmentId());
        assertEquals(10L, result.studentId());
        assertEquals(20L, result.courseId());
        assertEquals(AttendanceStatus.PRESENT, result.status());
    }

    @Test
    void testRecordAttendanceCorrectsSameDayInsteadOfCreatingADuplicateRow() {

        Enrollment enrollment = enrollment(1L);
        LocalDate date = LocalDate.now();
        AttendanceRecord existingRecord =
                new AttendanceRecord(enrollment, date, AttendanceStatus.ABSENT);
        AttendanceRequest request = new AttendanceRequest(1L, date, AttendanceStatus.LATE);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(repository.findByEnrollment_IdAndAttendanceDate(1L, date))
                .thenReturn(Optional.of(existingRecord));

        ArgumentCaptor<AttendanceRecord> captor = ArgumentCaptor.forClass(AttendanceRecord.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceResponseDTO result = attendanceService.recordAttendance(request);

        // The same record is reused and corrected, not a new one created.
        assertEquals(existingRecord, captor.getValue());
        assertEquals(AttendanceStatus.LATE, result.status());
    }


    // GET FOR ENROLLMENT

    @Test
    void testGetAttendanceForEnrollmentNotFoundThrows() {

        when(enrollmentRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                EnrollmentNotFoundException.class,
                () -> attendanceService.getAttendanceForEnrollment(99L));
    }

    @Test
    void testGetAttendanceForEnrollmentReturnsOrderedList() {

        Enrollment enrollment = enrollment(1L);
        AttendanceRecord r1 = new AttendanceRecord(
                enrollment, LocalDate.of(2026, 1, 1), AttendanceStatus.PRESENT);
        AttendanceRecord r2 = new AttendanceRecord(
                enrollment, LocalDate.of(2026, 1, 2), AttendanceStatus.ABSENT);

        when(enrollmentRepository.existsById(1L)).thenReturn(true);
        when(repository.findByEnrollment_IdOrderByAttendanceDate(1L)).thenReturn(List.of(r1, r2));

        List<AttendanceResponseDTO> result = attendanceService.getAttendanceForEnrollment(1L);

        assertEquals(2, result.size());
        assertEquals(AttendanceStatus.PRESENT, result.get(0).status());
        assertEquals(AttendanceStatus.ABSENT, result.get(1).status());
    }
}
