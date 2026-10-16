package com.suresh.sms.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A read-only record of something that happened for a student - a grade or
 * an attendance entry being recorded - written by {@link
 * com.suresh.sms.messaging.NotificationListener} as it consumes events off
 * RabbitMQ (see {@link com.suresh.sms.config.RabbitMQConfig}), not by the
 * request that originally recorded the grade or attendance.
 *
 * <p>Deliberately flat rather than a {@code @ManyToOne} to {@link Student}:
 * the listener has only the IDs and names carried on the event, and keeping
 * this table independent of the student/enrollment tables means it stays
 * readable even if the record it refers to is later changed or removed.
 */
@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    // The id of the Grade or AttendanceRecord this notification is about.
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(Long studentId, String studentName, NotificationType type,
            String message, Long referenceId) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.type = type;
        this.message = message;
        this.referenceId = referenceId;
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
