package com.suresh.sms.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.suresh.sms.config.RabbitMQConfig;
import com.suresh.sms.entity.Notification;
import com.suresh.sms.entity.NotificationType;
import com.suresh.sms.event.AttendanceRecordedEvent;
import com.suresh.sms.event.GradeRecordedEvent;
import com.suresh.sms.repository.NotificationRepository;

/**
 * Consumes {@code GradeRecordedEvent}/{@code AttendanceRecordedEvent}
 * messages off {@code RabbitMQConfig.NOTIFICATIONS_QUEUE} and turns each
 * into a {@link Notification} row - the asynchronous side effect that
 * decouples "a grade was recorded" from "something happened because of it",
 * separate from the request that did the recording.
 *
 * <p>One {@code @RabbitListener} class with two {@code @RabbitHandler}
 * methods, chosen by the payload type {@code Jackson2JsonMessageConverter}
 * resolves the message to - both event types share the one queue.
 *
 * <p>Any exception thrown here is treated by the listener container as a
 * rejection (see {@code RabbitMQConfig}'s {@code
 * defaultRequeueRejected(false)}), which RabbitMQ then routes to the dead-
 * letter queue rather than redelivering forever.
 */
@Component
@RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
public class NotificationListener {

    @Autowired
    private NotificationRepository repository;

    @RabbitHandler
    public void onGradeRecorded(GradeRecordedEvent event) {

        String message = String.format(
                "Grade recorded for %s in %s: %s",
                event.studentName(), event.courseTitle(), event.score());

        repository.save(new Notification(
                event.studentId(), event.studentName(), NotificationType.GRADE,
                message, event.gradeId()));
    }

    @RabbitHandler
    public void onAttendanceRecorded(AttendanceRecordedEvent event) {

        String message = String.format(
                "Attendance recorded for %s in %s on %s: %s",
                event.studentName(), event.courseTitle(), event.date(), event.status());

        repository.save(new Notification(
                event.studentId(), event.studentName(), NotificationType.ATTENDANCE,
                message, event.attendanceId()));
    }
}
