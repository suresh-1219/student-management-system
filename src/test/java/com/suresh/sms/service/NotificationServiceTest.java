package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suresh.sms.dto.NotificationDTO;
import com.suresh.sms.entity.Notification;
import com.suresh.sms.entity.NotificationType;
import com.suresh.sms.exception.StudentNotFoundException;
import com.suresh.sms.repository.NotificationRepository;
import com.suresh.sms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void testGetNotificationsForStudentNotFoundThrows() {

        when(studentRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                StudentNotFoundException.class,
                () -> notificationService.getNotificationsForStudent(99L));
    }

    @Test
    void testGetNotificationsForStudentReturnsNewestFirst() {

        Notification n1 = new Notification(
                10L, "Asha", NotificationType.GRADE, "Grade recorded for Asha in CS101: 91.00", 1L);
        Notification n2 = new Notification(
                10L, "Asha", NotificationType.ATTENDANCE,
                "Attendance recorded for Asha in CS101 on 2026-01-02: PRESENT", 2L);

        when(studentRepository.existsById(10L)).thenReturn(true);
        when(repository.findByStudentIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(n2, n1));

        List<NotificationDTO> result = notificationService.getNotificationsForStudent(10L);

        assertEquals(2, result.size());
        assertEquals(NotificationType.ATTENDANCE, result.get(0).type());
        assertEquals(NotificationType.GRADE, result.get(1).type());
    }
}
