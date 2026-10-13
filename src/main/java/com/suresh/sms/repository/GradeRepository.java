package com.suresh.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suresh.sms.entity.Grade;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findByEnrollment_Id(Long enrollmentId);

    List<Grade> findByEnrollment_Student_Id(Long studentId);
}
