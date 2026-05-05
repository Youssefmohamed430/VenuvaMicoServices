package com.example.registration_service.Messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publishes registration events to RabbitMQ.
 *
 * Events published:
 *   - registration.created → Notification Service creates a confirmation notification for user
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish registration.created event after a user successfully registers to an event.
     * Notification Service will consume this and send a confirmation notification.
     *
     * Async — fire-and-forget. Caller does not wait.
     */
    public void publishRegistrationCreated(RegistrationCreatedEvent event) {
        log.info("[RabbitMQ] Publishing registration.created — userId={}, eventId={}",
                event.getUserId(), event.getEventId());
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.REGISTRATION_CREATED_ROUTING_KEY,
            event
        );
        log.info("[RabbitMQ] Published registration.created successfully");
    }
}
