package com.example.notif_service.Controllers;

import com.example.notif_service.Abstractions.Result;
import com.example.notif_service.Config.ResponseUtility;
import com.example.notif_service.DTOs.NotifDTO;
import com.example.notif_service.Services.INotifService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor

public class NotificationController {   

    private final INotifService notifService;

    // GET /api/notifications/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<?> getNotifs(@PathVariable int userId) {
        Result<List<NotifDTO>> result = notifService.getNotifsById(userId);
        return ResponseUtility.toResponse(result);
    }

    // PUT /api/notifications/mark-read/{notifId}
    @PutMapping("/mark-read/{notifId}")
    public ResponseEntity<?> markAsRead(@PathVariable int notifId) {
        Result<NotifDTO> result = notifService.markNotifAsRead(notifId);
        return ResponseUtility.toResponse(result);
    }
}