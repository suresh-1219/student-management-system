package com.suresh.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suresh.sms.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByName(String name);

}