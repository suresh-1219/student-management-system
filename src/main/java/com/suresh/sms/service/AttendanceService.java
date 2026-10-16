package com.suresh.sms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.dto.AttendanceRequest;
import com.suresh.sms.dto.AttendanceResponseDTO;
import com.suresh.sms.entity.AttendanceRecord;
import com.suresh.sms.entity.Enrollment;
import com.suresh.sms.event.AttendanceRecordedEvent;
import com.suresh.sms.exception.EnrollmentNotFoundException;
import com.suresh.sms.messaging.EventPublisher;
import com.suresh.sms.repository.AttendanceRecordRepository;
import com.suresh.sms.repository.EnrollmentRepository;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRecordRepository repository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Records one enrollment's attendance status for one day, or corrects
     * it if that day was already recorded (see
     * V6__teachers_grades_attendance.sql's uk_attendance_enrollment_date:
     * at most one row per enrollment per day).
     */
    @Transactional
    public AttendanceResponseDTO recordAttendance(AttendanceRequest request) {

        Enrollment enrollment = enrollmentRepository.findById(request.enrollmentId())
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "Enrollment not found with id : " + request.enrollmentId()));

        AttendanceRecord record = repository
                .findByEnrollment_IdAndAttendanceDate(enrollment.getId(), request.date())
                .orElseGet(() ->
                        new AttendanceRecord(enrollment, request.date(), request.status()));

        record.setStatus(request.status());

        AttendanceRecord saved = repository.save(record);

        AttendanceResponseDTO dto = convertToDTO(saved);

        // See the equivalent comment in GradeService.recordGrade: published
        // before commit, a deliberate simplification since this only drives
        // a best-effort Notification row.
        eventPublisher.publishAttendanceRecorded(new AttendanceRecordedEvent(
                dto.id(), dto.enrollmentId(), dto.studentId(), enrollment.getStudent().getName(),
                dto.courseId(), dto.courseTitle(), dto.date(), dto.status(), saved.getUpdatedAt()));

        return dto;
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getAttendanceForEnrollment(Long enrollmentId) {

        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new EnrollmentNotFoundException(
                    "Enrollment not found with id : " + enrollmentId);
        }

        return repository.findByEnrollment_IdOrderByAttendanceDate(enrollmentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AttendanceResponseDTO convertToDTO(AttendanceRecord record) {

        Enrollment enrollment = record.getEnrollment();

        return new AttendanceResponseDTO(
                record.getId(),
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(),
                record.getAttendanceDate(),
                record.getStatus()
        );
    }
}
