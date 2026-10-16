package com.suresh.sms.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.suresh.sms.config.RabbitMQConfig;
import com.suresh.sms.entity.AttendanceStatus;
import com.suresh.sms.event.AttendanceRecordedEvent;
import com.suresh.sms.event.GradeRecordedEvent;

/**
 * {@code EventPublisher} is documented as a best-effort, "degrades
 * gracefully" side effect (see {@code CacheConfig}'s equivalent treatment of
 * Redis): a broker failure must never propagate out of these methods and
 * fail the HTTP request that triggered them. That guarantee is exactly what
 * these tests check.
 */
@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventPublisher eventPublisher;

    private GradeRecordedEvent gradeEvent() {
        return new GradeRecordedEvent(
                1L, 2L, 10L, "Asha", 20L, "CS101",
                new BigDecimal("91.00"), LocalDateTime.now());
    }

    private AttendanceRecordedEvent attendanceEvent() {
        return new AttendanceRecordedEvent(
                1L, 2L, 10L, "Asha", 20L, "CS101",
                LocalDate.now(), AttendanceStatus.PRESENT, LocalDateTime.now());
    }

    @Test
    void testPublishGradeRecordedSendsToTheEventsExchangeWithTheGradeRoutingKey() {

        eventPublisher.publishGradeRecorded(gradeEvent());

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EVENTS_EXCHANGE),
                eq(RabbitMQConfig.GRADE_RECORDED_ROUTING_KEY),
                any(GradeRecordedEvent.class));
    }

    @Test
    void testPublishAttendanceRecordedSendsToTheEventsExchangeWithTheAttendanceRoutingKey() {

        eventPublisher.publishAttendanceRecorded(attendanceEvent());

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EVENTS_EXCHANGE),
                eq(RabbitMQConfig.ATTENDANCE_RECORDED_ROUTING_KEY),
                any(AttendanceRecordedEvent.class));
    }

    @Test
    void testPublishGradeRecordedSwallowsABrokerFailureInsteadOfThrowing() {

        doThrow(new AmqpException("broker unreachable"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertDoesNotThrow(() -> eventPublisher.publishGradeRecorded(gradeEvent()));
    }

    @Test
    void testPublishAttendanceRecordedSwallowsABrokerFailureInsteadOfThrowing() {

        doThrow(new AmqpException("broker unreachable"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertDoesNotThrow(() -> eventPublisher.publishAttendanceRecorded(attendanceEvent()));
    }
}
