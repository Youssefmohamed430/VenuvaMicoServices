package com.example.registration_service.Messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Registration Service.
 *
 * Publishes:
 *   - registration.created  → Notification Service listens to send confirmation notification
 *
 * Consumes:
 *   - payment.success       → Updates registration status from PENDING → PAID
 *   - payment.failed        → Updates registration status from PENDING → FAILED (optional)
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "venuva.events";

    // Queue: listen for successful payments from Payment Service
    public static final String PAYMENT_SUCCESS_QUEUE = "registration.payment.success";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";

    // Routing key: publish when a new registration is created
    public static final String REGISTRATION_CREATED_ROUTING_KEY = "registration.created";

    @Bean
    public TopicExchange venuvExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(PAYMENT_SUCCESS_QUEUE, true);
    }

    @Bean
    public Binding paymentSuccessBinding(Queue paymentSuccessQueue, TopicExchange venuvExchange) {
        return BindingBuilder.bind(paymentSuccessQueue).to(venuvExchange).with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.support.converter.MessageConverter jsonMessageConverter() {
        return new org.springframework.amqp.support.converter.JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
