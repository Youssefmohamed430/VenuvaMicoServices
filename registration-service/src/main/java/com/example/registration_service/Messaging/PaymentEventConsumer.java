package com.example.registration_service.Messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.registration_service.Models.Registration;
import com.example.registration_service.Repos.RegistrationRepository;
import com.example.registration_service.RegistrationStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ consumer for payment events.
 *
 * Listens to:
 *   - payment.success (queue: registration.payment.success)
 *     → Updates the registration status from PENDING to PAID
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final RegistrationRepository registrationRepository;

    /**
     * Called asynchronously when Payment Service confirms a successful payment.
     * Updates the registration status from PENDING → PAID.
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("[RabbitMQ] Received payment.success — userId={}, eventId={}, orderId={}",
                event.getUserId(), event.getEventId(), event.getOrderId());

        Registration registration = registrationRepository.findByEventIdAndUserId(
            event.getEventId(), event.getUserId()
        );

        if (registration == null) {
            log.warn("[RabbitMQ] PaymentEventConsumer — No registration found for userId={}, eventId={}",
                    event.getUserId(), event.getEventId());
            return;
        }

        registration.setRegistrationStatus(RegistrationStatus.PAID);
        registrationRepository.save(registration);

        log.info("[RabbitMQ] PaymentEventConsumer — Registration status updated to PAID for userId={}, eventId={}",
                event.getUserId(), event.getEventId());
    }
}
