package com.example.paymentservice.Messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publishes payment outcome events to RabbitMQ.
 *
 * Published events:
 *   - payment.success → Registration Service + Notification Service
 *   - payment.failed  → Notification Service (optional)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish payment.success after Paymob HMAC webhook is verified.
     * Async — fire-and-forget. Callback must return 200 immediately.
     */
    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        log.info("[RabbitMQ] Publishing payment.success — userId={}, eventId={}, orderId={}",
                event.getUserId(), event.getEventId(), event.getOrderId());
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY,
            event
        );
        log.info("[RabbitMQ] Published payment.success successfully");
    }
}
