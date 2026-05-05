package com.example.notif_service.Messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.notif_service.DTOs.CreateNotificationDto;
import com.example.notif_service.Services.INotifService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ consumer for Event Service messages.
 *
 * Listens to: event.created (queue: notif.event.created)
 * Action: Creates a broadcast notification.
 *         Note: Since we don't have a "send to all users" list here,
 *               we save a global notification. Frontend polls GET /api/notifications/{userId}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final INotifService notifService;

    @RabbitListener(queues = RabbitMQConfig.EVENT_CREATED_QUEUE)
    public void handleEventCreated(EventCreatedMessage message) {
        log.info("[RabbitMQ] EventConsumer.handleEventCreated() — eventId={}, title={}",
                message.getEventId(), message.getTitle());

        // Create a system-wide notification (userId=0 = broadcast marker)
        // Individual user notifications can be created by Registration Service
        CreateNotificationDto dto = new CreateNotificationDto();
        dto.setUserId(0); // 0 = broadcast / system notification
        dto.setEventId(message.getEventId());
        dto.setMessage(message.getMessage() != null
            ? message.getMessage()
            : "New event available: " + message.getTitle() + " on " + message.getDate() + " at " + message.getLocation());

        notifService.createNotification(dto);

        log.info("[RabbitMQ] EventConsumer — Notification created for event.created event");
    }
}
