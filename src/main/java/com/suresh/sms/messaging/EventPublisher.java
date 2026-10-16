package com.suresh.sms.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.suresh.sms.config.RabbitMQConfig;
import com.suresh.sms.event.AttendanceRecordedEvent;
import com.suresh.sms.event.GradeRecordedEvent;

/**
 * Publishes domain events to RabbitMQ for {@code NotificationListener} to
 * pick up.
 *
 * <p>Publishing is a side effect of recording a grade or attendance entry,
 * not the point of the request - a broker outage should not turn a
 * successful grade write into a failed HTTP request. Every publish is
 * wrapped so a {@code RabbitTemplate} failure (broker down, channel error)
 * is logged and swallowed here rather than propagated to the caller, the
 * same "optional, degrades gracefully" treatment {@code CacheConfig} gives
 * Redis.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishGradeRecorded(GradeRecordedEvent event) {

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENTS_EXCHANGE,
                    RabbitMQConfig.GRADE_RECORDED_ROUTING_KEY,
                    event);
        } catch (Exception ex) {
            log.warn("Failed to publish GradeRecordedEvent for grade {}: {}",
                    event.gradeId(), ex.getMessage());
        }
    }

    public void publishAttendanceRecorded(AttendanceRecordedEvent event) {

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENTS_EXCHANGE,
                    RabbitMQConfig.ATTENDANCE_RECORDED_ROUTING_KEY,
                    event);
        } catch (Exception ex) {
            log.warn("Failed to publish AttendanceRecordedEvent for record {}: {}",
                    event.attendanceId(), ex.getMessage());
        }
    }
}
