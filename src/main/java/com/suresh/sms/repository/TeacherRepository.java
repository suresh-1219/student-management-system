package com.suresh.sms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suresh.sms.entity.Teacher;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    boolean existsByEmail(String email);

    /** True if a DIFFERENT teacher (other id) already uses this email. */
    boolean existsByEmailAndIdNot(String email, Long id);
}
