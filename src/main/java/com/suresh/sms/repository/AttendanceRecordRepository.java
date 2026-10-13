package com.suresh.sms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suresh.sms.entity.AttendanceRecord;
import com.suresh.sms.entity.AttendanceStatus;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByEnrollment_IdAndAttendanceDate(
            Long enrollmentId, LocalDate attendanceDate);

    List<AttendanceRecord> findByEnrollment_IdOrderByAttendanceDate(Long enrollmentId);

    long countByEnrollment_Id(Long enrollmentId);

    long countByEnrollment_IdAndStatus(Long enrollmentId, AttendanceStatus status);
}
