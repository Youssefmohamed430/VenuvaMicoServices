package com.example.notif_service.Messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Notification Service.
 *
 * Consumes:
 *   - event.created        → Send notification to all users about new event
 *   - event.updated        → Send notification to registered users about updated event
 *   - registration.created → Send confirmation notification to registered user
 *   - payment.success      → Send payment confirmation notification to user
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "venuva.events";

    // Queue names — unique to this service so each service gets its own copy of messages
    public static final String EVENT_CREATED_QUEUE      = "notif.event.created";
    public static final String EVENT_UPDATED_QUEUE      = "notif.event.updated";
    public static final String REGISTRATION_CREATED_QUEUE = "notif.registration.created";
    public static final String PAYMENT_SUCCESS_QUEUE    = "notif.payment.success";

    // Routing keys (must match publisher)
    public static final String EVENT_CREATED_KEY        = "event.created";
    public static final String EVENT_UPDATED_KEY        = "event.updated";
    public static final String REGISTRATION_CREATED_KEY = "registration.created";
    public static final String PAYMENT_SUCCESS_KEY      = "payment.success";

    @Bean
    public TopicExchange venuvExchange() {
        return new TopicExchange(EXCHANGE);
    }

    // ===== event.created =====
    @Bean
    public Queue eventCreatedQueue() {
        return new Queue(EVENT_CREATED_QUEUE, true);
    }

    @Bean
    public Binding eventCreatedBinding(Queue eventCreatedQueue, TopicExchange venuvExchange) {
        return BindingBuilder.bind(eventCreatedQueue).to(venuvExchange).with(EVENT_CREATED_KEY);
    }

    // ===== event.updated =====
    @Bean
    public Queue eventUpdatedQueue() {
        return new Queue(EVENT_UPDATED_QUEUE, true);
    }

    @Bean
    public Binding eventUpdatedBinding(Queue eventUpdatedQueue, TopicExchange venuvExchange) {
        return BindingBuilder.bind(eventUpdatedQueue).to(venuvExchange).with(EVENT_UPDATED_KEY);
    }

    // ===== registration.created =====
    @Bean
    public Queue registrationCreatedQueue() {
        return new Queue(REGISTRATION_CREATED_QUEUE, true);
    }

    @Bean
    public Binding registrationCreatedBinding(Queue registrationCreatedQueue, TopicExchange venuvExchange) {
        return BindingBuilder.bind(registrationCreatedQueue).to(venuvExchange).with(REGISTRATION_CREATED_KEY);
    }

    // ===== payment.success =====
    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(PAYMENT_SUCCESS_QUEUE, true);
    }

    @Bean
    public Binding paymentSuccessBinding(Queue paymentSuccessQueue, TopicExchange venuvExchange) {
        return BindingBuilder.bind(paymentSuccessQueue).to(venuvExchange).with(PAYMENT_SUCCESS_KEY);
    }

    @Bean
    public org.springframework.amqp.support.converter.MessageConverter jsonMessageConverter() {
        return new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
