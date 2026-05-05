package com.example.notif_service.Controllers;

import com.example.AOP.Annotation.HandleException;
import com.example.AOP.Annotation.Loggable;
import com.example.notif_service.Abstractions.Result;
import com.example.notif_service.Config.ResponseUtility;
import com.example.notif_service.DTOs.CreateNotificationDto;
import com.example.notif_service.DTOs.NotifDTO;
import com.example.notif_service.Services.INotifService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final INotifService notifService;

    /**
     * GET /api/notifications/{userId}
     * Retrieve all notifications for a user.
     */
    @GetMapping("/{userId}")
    @HandleException
    @Loggable(value = "GetNotifications", logArguments = true, logResult = false)
    public ResponseEntity<?> getNotifs(@PathVariable int userId) {
        log.info("[USER] NotificationController.getNotifs() — userId={}", userId);
        Result<List<NotifDTO>> result = notifService.getNotifsById(userId);
        return ResponseUtility.toResponse(result);
    }

    /**
     * PUT /api/notifications/mark-read/{notifId}
     * Mark a notification as read.
     */
    @PutMapping("/mark-read/{notifId}")
    @HandleException
    @Loggable(value = "MarkNotificationRead", logArguments = true, logResult = false)
    public ResponseEntity<?> markAsRead(@PathVariable int notifId) {
        log.info("[USER] NotificationController.markAsRead() — notifId={}", notifId);
        Result<NotifDTO> result = notifService.markNotifAsRead(notifId);
        return ResponseUtility.toResponse(result);
    }

    /**
     * POST /api/notifications
     * Create a new notification for a specific user.
     * Used by other microservices (Event, Payment, Registration) for inter-service calls.
     */
    @PostMapping
    @HandleException
    @Loggable(value = "CreateNotification", logArguments = false, logResult = false)
    public ResponseEntity<?> createNotification(@Valid @RequestBody CreateNotificationDto dto) {
        log.info("[INTERNAL] NotificationController.createNotification() — userId={}", dto.getUserId());
        Result<NotifDTO> result = notifService.createNotification(dto);
        return ResponseUtility.toResponse(result, HttpStatus.CREATED);
    }
}