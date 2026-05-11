package com.example.paymentservice.Messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Payment Service.
 *
 * Publishes:
 *   - payment.success → Registration Service updates status, Notification Service sends confirmation
 *   - payment.failed  → Notification Service can inform user (optional)
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "venuva.events";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";
    public static final String PAYMENT_FAILED_ROUTING_KEY  = "payment.failed";

    @Bean
    public TopicExchange venuvExchange() {
        return new TopicExchange(EXCHANGE);
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
