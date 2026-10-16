package com.suresh.sms.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.ApiResponse;
import com.suresh.sms.dto.NotificationDTO;
import com.suresh.sms.service.NotificationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/notifications")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    @Autowired
    private NotificationService service;


    // LIST A STUDENT'S NOTIFICATIONS (newest first), written asynchronously
    // by NotificationListener as it consumes grade/attendance events

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotificationsForStudent(
            @PathVariable Long studentId) {

        List<NotificationDTO> notifications = service.getNotificationsForStudent(studentId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Notifications retrieved successfully", notifications));
    }
}
