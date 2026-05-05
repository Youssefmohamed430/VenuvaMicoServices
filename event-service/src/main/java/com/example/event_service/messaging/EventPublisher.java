package com.example.event_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.event_service.dto.EventDtos.EventCreatedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE = "venuva.events";
    public static final String EVENT_CREATED_ROUTING_KEY = "event.created";

    /**
     * بنبعت event لما event جديد يتعمل
     * Notification Service هيسمع الـ message دي
     * وهيبعت notification لكل الـ attendees
     */
    public void publishEventCreated(EventCreatedEvent event) {
        
        log.info("[RabbitMQ] Publishing event.created — eventId={}, title={}",
                event.getEventId(), event.getTitle());

        rabbitTemplate.convertAndSend(EXCHANGE, EVENT_CREATED_ROUTING_KEY, event);
        log.info("[RabbitMQ] Published event.created successfully");
    }
}
