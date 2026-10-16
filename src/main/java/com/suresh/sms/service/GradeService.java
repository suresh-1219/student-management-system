package com.suresh.sms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.dto.GradeRequest;
import com.suresh.sms.dto.GradeResponseDTO;
import com.suresh.sms.entity.Enrollment;
import com.suresh.sms.entity.Grade;
import com.suresh.sms.event.GradeRecordedEvent;
import com.suresh.sms.exception.EnrollmentNotFoundException;
import com.suresh.sms.messaging.EventPublisher;
import com.suresh.sms.repository.EnrollmentRepository;
import com.suresh.sms.repository.GradeRepository;

@Service
public class GradeService {

    @Autowired
    private GradeRepository repository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Records a student's grade for one enrollment, or corrects it if one
     * was already recorded (see V6__teachers_grades_attendance.sql's
     * uk_grades_enrollment: a student has at most one grade per course).
     */
    @Transactional
    public GradeResponseDTO recordGrade(GradeRequest request) {

        Enrollment enrollment = enrollmentRepository.findById(request.enrollmentId())
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "Enrollment not found with id : " + request.enrollmentId()));

        Grade grade = repository.findByEnrollment_Id(enrollment.getId())
                .orElseGet(() -> new Grade(enrollment, request.score()));

        grade.setScore(request.score());

        Grade saved = repository.save(grade);

        GradeResponseDTO dto = convertToDTO(saved);

        // Published before this transaction commits: a known simplification
        // (the "dual write" problem) rather than a transactional outbox -
        // acceptable here because EventPublisher only drives a best-effort
        // Notification row, not anything that must stay perfectly
        // consistent with the grade itself.
        eventPublisher.publishGradeRecorded(new GradeRecordedEvent(
                dto.id(), dto.enrollmentId(), dto.studentId(), dto.studentName(),
                dto.courseId(), dto.courseTitle(), dto.score(), dto.gradedAt()));

        return dto;
    }

    @Transactional(readOnly = true)
    public GradeResponseDTO getGradeForEnrollment(Long enrollmentId) {

        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new EnrollmentNotFoundException(
                    "Enrollment not found with id : " + enrollmentId);
        }

        return repository.findByEnrollment_Id(enrollmentId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<GradeResponseDTO> getGradesForStudent(Long studentId) {

        return repository.findByEnrollment_Student_Id(studentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private GradeResponseDTO convertToDTO(Grade grade) {

        Enrollment enrollment = grade.getEnrollment();

        return new GradeResponseDTO(
                grade.getId(),
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getName(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(),
                grade.getScore(),
                grade.getUpdatedAt()
        );
    }
}
