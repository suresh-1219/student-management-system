-- A denormalized read model of what happened for a student, populated
-- asynchronously by NotificationListener as it consumes GradeRecordedEvent
-- and AttendanceRecordedEvent messages off RabbitMQ (see RabbitMQConfig).
-- Deliberately not a foreign key to students/enrollments: a notification
-- should remain readable even if, say, the originating enrollment is later
-- deleted, and the listener never needs to join back to those tables.
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    message VARCHAR(500) NOT NULL,
    reference_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_notifications_student_id (student_id)
);
