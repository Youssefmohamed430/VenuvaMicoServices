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

        String msgText = message.getMessage() != null
            ? message.getMessage()
            : "New event available: " + message.getTitle() + " on " + message.getDate() + " at " + message.getLocation();

        notifService.sendNotificationToAllAttendees(msgText, message.getEventId(), message.getTitle());

        log.info("[RabbitMQ] EventConsumer — Notification sent for event.created");
    }

    @RabbitListener(queues = RabbitMQConfig.EVENT_UPDATED_QUEUE)
    public void handleEventUpdated(EventUpdatedMessage message) {
        log.info("[RabbitMQ] EventConsumer.handleEventUpdated() — eventId={}, title={}",
                message.getEventId(), message.getTitle());

        String msgText = message.getMessage() != null
            ? message.getMessage()
            : "Event Updated: " + message.getTitle() + " on " + message.getDate() + " at " + message.getLocation();

        notifService.sendNotificationToEventRegistrants(message.getEventId(), msgText, message.getTitle());

        log.info("[RabbitMQ] EventConsumer — Notification sent for event.updated");
    }
}
