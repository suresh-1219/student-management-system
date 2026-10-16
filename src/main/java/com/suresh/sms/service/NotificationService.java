package com.suresh.sms.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.dto.NotificationDTO;
import com.suresh.sms.entity.Notification;
import com.suresh.sms.exception.StudentNotFoundException;
import com.suresh.sms.repository.NotificationRepository;
import com.suresh.sms.repository.StudentRepository;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsForStudent(Long studentId) {

        if (!studentRepository.existsById(studentId)) {
            throw new StudentNotFoundException("Student not found with id : " + studentId);
        }

        return repository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private NotificationDTO convertToDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.getCreatedAt());
    }
}
