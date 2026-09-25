package com.suresh.sms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.suresh.sms.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByCode(String code);

    /** True if a DIFFERENT course (other id) already uses this code. */
    boolean existsByCodeAndIdNot(String code, Long id);

    /**
     * Same lookup as {@link #findById}, but takes a row lock (SELECT ... FOR
     * UPDATE) that is held until the enclosing transaction commits.
     *
     * <p>Enrolling counts existing seats and then inserts a new row - two
     * requests racing for the last seat could otherwise both pass the
     * capacity check before either commits. Locking the course row makes
     * the second request wait for the first to finish, so it recounts
     * against the now-accurate number and is correctly rejected if the
     * course is full.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);
}
