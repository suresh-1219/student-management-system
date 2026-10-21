package com.suresh.sms.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.suresh.sms.config.RabbitMQConfig;
import com.suresh.sms.event.AttendanceRecordedEvent;
import com.suresh.sms.event.GradeRecordedEvent;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

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
 *
 * <p>Each publish also goes through a retry + circuit breaker (Drop G4;
 * see {@code ResilienceConfig}) before that swallow: a couple of quick
 * retries ride out a blip that a bare try/catch would have given up on
 * immediately, and once the broker is clearly down, the circuit opens so
 * every subsequent publish fails (and gets logged/swallowed, same as
 * always) in microseconds rather than each one waiting out its own
 * connection attempt.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    @Qualifier("eventPublisherCircuitBreaker")
    private CircuitBreaker circuitBreaker;

    @Autowired
    @Qualifier("eventPublisherRetry")
    private Retry retry;

    public void publishGradeRecorded(GradeRecordedEvent event) {

        publish("GradeRecordedEvent for grade " + event.gradeId(), () ->
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EVENTS_EXCHANGE,
                        RabbitMQConfig.GRADE_RECORDED_ROUTING_KEY,
                        event));
    }

    public void publishAttendanceRecorded(AttendanceRecordedEvent event) {

        publish("AttendanceRecordedEvent for record " + event.attendanceId(), () ->
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EVENTS_EXCHANGE,
                        RabbitMQConfig.ATTENDANCE_RECORDED_ROUTING_KEY,
                        event));
    }

    /**
     * Runs {@code publishAction} through the retry + circuit breaker, then
     * swallows whatever is left - a retry exhausted, or the circuit simply
     * refusing the call (a {@code CallNotPermittedException}) - exactly
     * the way a bare try/catch around the broker call used to, before this
     * drop.
     */
    private void publish(String eventDescription, Runnable publishAction) {

        Runnable resilient = Retry.decorateRunnable(
                retry, CircuitBreaker.decorateRunnable(circuitBreaker, publishAction));

        try {
            resilient.run();
        } catch (Exception ex) {
            log.warn("Failed to publish {}: {}", eventDescription, ex.getMessage());
        }
    }
}
