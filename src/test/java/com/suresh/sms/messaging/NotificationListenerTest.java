package com.suresh.sms.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suresh.sms.entity.AttendanceStatus;
import com.suresh.sms.entity.Notification;
import com.suresh.sms.entity.NotificationType;
import com.suresh.sms.event.AttendanceRecordedEvent;
import com.suresh.sms.event.GradeRecordedEvent;
import com.suresh.sms.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationListener listener;

    @Test
    void testOnGradeRecordedSavesAGradeNotificationForTheStudent() {

        GradeRecordedEvent event = new GradeRecordedEvent(
                1L, 2L, 10L, "Asha", 20L, "CS101",
                new BigDecimal("91.00"), LocalDateTime.now());

        listener.onGradeRecorded(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(10L, saved.getStudentId());
        assertEquals("Asha", saved.getStudentName());
        assertEquals(NotificationType.GRADE, saved.getType());
        assertEquals(1L, saved.getReferenceId());
        assertEquals(true, saved.getMessage().contains("91.00"));
        assertEquals(true, saved.getMessage().contains("CS101"));
    }

    @Test
    void testOnAttendanceRecordedSavesAnAttendanceNotificationForTheStudent() {

        AttendanceRecordedEvent event = new AttendanceRecordedEvent(
                5L, 2L, 10L, "Asha", 20L, "CS101",
                LocalDate.of(2026, 1, 2), AttendanceStatus.PRESENT, LocalDateTime.now());

        listener.onAttendanceRecorded(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(10L, saved.getStudentId());
        assertEquals(NotificationType.ATTENDANCE, saved.getType());
        assertEquals(5L, saved.getReferenceId());
        assertEquals(true, saved.getMessage().contains("PRESENT"));
        assertEquals(true, saved.getMessage().contains("CS101"));
    }
}
