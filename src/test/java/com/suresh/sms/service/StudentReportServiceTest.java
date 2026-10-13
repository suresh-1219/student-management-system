package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suresh.sms.dto.CourseReportEntry;
import com.suresh.sms.dto.StudentReportDTO;
import com.suresh.sms.entity.AttendanceStatus;
import com.suresh.sms.entity.Course;
import com.suresh.sms.entity.Enrollment;
import com.suresh.sms.entity.Grade;
import com.suresh.sms.entity.Student;
import com.suresh.sms.exception.StudentNotFoundException;
import com.suresh.sms.repository.AttendanceRecordRepository;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.GradeRepository;
import com.suresh.sms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentReportServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private AttendanceRecordRepository attendanceRepository;

    @InjectMocks
    private StudentReportService studentReportService;

    private Student student(Long id, String name) {
        return new Student(id, name, name.toLowerCase() + "@mail.com", "CS101", BigDecimal.ZERO);
    }

    private Enrollment enrollment(Long id, Student student, Course course) {
        Enrollment enrollment = new Enrollment(student, course);
        enrollment.setId(id);
        return enrollment;
    }

    @Test
    void testGetReportForStudentNotFound() {

        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                StudentNotFoundException.class,
                () -> studentReportService.getReportForStudent(99L));
    }

    @Test
    void testGradeIsNullWhenNoneHasBeenRecordedYet() {

        Student student = student(10L, "Asha");
        Course course = new Course(20L, "CS101", "Intro to CS", 30);
        Enrollment enrollment = enrollment(1L, student, course);

        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudent_Id(10L)).thenReturn(List.of(enrollment));
        when(gradeRepository.findByEnrollment_Id(1L)).thenReturn(Optional.empty());
        when(attendanceRepository.countByEnrollment_Id(1L)).thenReturn(0L);

        StudentReportDTO report = studentReportService.getReportForStudent(10L);
        CourseReportEntry entry = report.courses().get(0);

        assertNull(entry.grade());
    }

    @Test
    void testAttendancePercentageIsNullWhenThereAreZeroAttendanceDays() {

        Student student = student(10L, "Asha");
        Course course = new Course(20L, "CS101", "Intro to CS", 30);
        Enrollment enrollment = enrollment(1L, student, course);

        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudent_Id(10L)).thenReturn(List.of(enrollment));
        when(gradeRepository.findByEnrollment_Id(1L)).thenReturn(Optional.empty());
        when(attendanceRepository.countByEnrollment_Id(1L)).thenReturn(0L);

        StudentReportDTO report = studentReportService.getReportForStudent(10L);
        CourseReportEntry entry = report.courses().get(0);

        // Zero days of data is "no data yet", not "0% attendance".
        assertNull(entry.attendancePercentage());
        assertEquals(0, entry.attendanceTotalDays());
    }

    @Test
    void testAttendancePercentageIsComputedCorrectlyWhenDataExists() {

        Student student = student(10L, "Asha");
        Course course = new Course(20L, "CS101", "Intro to CS", 30);
        Enrollment enrollment = enrollment(1L, student, course);
        Grade grade = new Grade(enrollment, new BigDecimal("91.00"));

        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudent_Id(10L)).thenReturn(List.of(enrollment));
        when(gradeRepository.findByEnrollment_Id(1L)).thenReturn(Optional.of(grade));
        when(attendanceRepository.countByEnrollment_Id(1L)).thenReturn(4L);
        when(attendanceRepository.countByEnrollment_IdAndStatus(1L, AttendanceStatus.PRESENT))
                .thenReturn(3L);

        StudentReportDTO report = studentReportService.getReportForStudent(10L);
        CourseReportEntry entry = report.courses().get(0);

        assertEquals(new BigDecimal("91.00"), entry.grade());
        assertEquals(4, entry.attendanceTotalDays());
        assertEquals(3, entry.attendancePresentDays());
        // 3 / 4 * 100 = 75.0
        assertEquals(0, new BigDecimal("75.0").compareTo(entry.attendancePercentage()));
    }
}
