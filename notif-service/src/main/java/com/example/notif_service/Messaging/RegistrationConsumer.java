package com.example.notif_service.Messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.notif_service.DTOs.CreateNotificationDto;
import com.example.notif_service.Services.INotifService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ consumer for Registration Service messages.
 *
 * Listens to: registration.created (queue: notif.registration.created)
 * Action: Sends a registration confirmation notification to the specific user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationConsumer {

    private final INotifService notifService;

    @RabbitListener(queues = RabbitMQConfig.REGISTRATION_CREATED_QUEUE)
    public void handleRegistrationCreated(RegistrationCreatedMessage message) {
        log.info("[RabbitMQ] RegistrationConsumer.handleRegistrationCreated() — userId={}, eventId={}",
                message.getUserId(), message.getEventId());

        CreateNotificationDto dto = new CreateNotificationDto();
        dto.setUserId(message.getUserId());
        dto.setEventId(message.getEventId());
        dto.setMessage(message.getMessage() != null
            ? message.getMessage()
            : "You have successfully registered for: " + message.getEventTitle());

        notifService.createNotification(dto);

        log.info("[RabbitMQ] RegistrationConsumer — Registration confirmation notification created for userId={}",
                message.getUserId());
    }
}
