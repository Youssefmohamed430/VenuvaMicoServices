package com.example.notif_service.Services;

import java.util.List;

import com.example.notif_service.Abstractions.Result;
import com.example.notif_service.DTOs.CreateNotificationDto;
import com.example.notif_service.DTOs.NotifDTO;

public interface INotifService {

    Result<Object> sendNotification(String message);

    Result<List<NotifDTO>> getNotifsById(int id);

    Result<NotifDTO> markNotifAsRead(int notifId);

    /**
     * Create a notification for a specific user.
     * Used by REST endpoint POST /api/notifications
     * and internally by RabbitMQ consumers.
     */
    Result<NotifDTO> createNotification(CreateNotificationDto dto);
}