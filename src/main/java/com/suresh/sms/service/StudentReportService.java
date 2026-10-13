package com.suresh.sms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.dto.CourseReportEntry;
import com.suresh.sms.dto.StudentReportDTO;
import com.suresh.sms.entity.AttendanceStatus;
import com.suresh.sms.entity.Course;
import com.suresh.sms.entity.Enrollment;
import com.suresh.sms.entity.Student;
import com.suresh.sms.exception.StudentNotFoundException;
import com.suresh.sms.repository.AttendanceRecordRepository;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.GradeRepository;
import com.suresh.sms.repository.StudentRepository;

/**
 * Assembles one student's report: every course they are enrolled in, the
 * grade for it (if recorded) and an attendance summary - by combining rows
 * from three otherwise-independent tables (Enrollment, Grade,
 * AttendanceRecord). A dedicated service, rather than a method bolted onto
 * one of those three, because it genuinely depends on all of them and
 * belongs to none.
 */
@Service
public class StudentReportService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRepository;

    @Transactional(readOnly = true)
    public StudentReportDTO getReportForStudent(Long studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(
                        "Student not found with id : " + studentId));

        List<CourseReportEntry> entries = enrollmentRepository.findByStudent_Id(studentId)
                .stream()
                .map(this::toEntry)
                .collect(Collectors.toList());

        return new StudentReportDTO(student.getId(), student.getName(), entries);
    }

    private CourseReportEntry toEntry(Enrollment enrollment) {

        Course course = enrollment.getCourse();

        BigDecimal grade = gradeRepository.findByEnrollment_Id(enrollment.getId())
                .map(com.suresh.sms.entity.Grade::getScore)
                .orElse(null);

        long totalDays = attendanceRepository.countByEnrollment_Id(enrollment.getId());
        long presentDays = attendanceRepository.countByEnrollment_IdAndStatus(
                enrollment.getId(), AttendanceStatus.PRESENT);

        // null (not 0) when there is no attendance data yet, so a caller can
        // tell "never recorded" apart from "recorded, and it was 0%".
        BigDecimal percentage = totalDays == 0
                ? null
                : BigDecimal.valueOf(presentDays)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalDays), 1, RoundingMode.HALF_UP);

        return new CourseReportEntry(
                enrollment.getId(),
                course.getId(),
                course.getCode(),
                course.getTitle(),
                grade,
                (int) totalDays,
                (int) presentDays,
                percentage
        );
    }
}
