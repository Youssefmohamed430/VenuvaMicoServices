package com.example.notif_service.Messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.notif_service.DTOs.CreateNotificationDto;
import com.example.notif_service.Services.INotifService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ consumer for Payment Service messages.
 *
 * Listens to: payment.success (queue: notif.payment.success)
 * Action: Sends a payment confirmation notification to the user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final INotifService notifService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessMessage message) {
        log.info("[RabbitMQ] PaymentConsumer.handlePaymentSuccess() — userId={}, eventId={}, orderId={}",
                message.getUserId(), message.getEventId(), message.getOrderId());

        CreateNotificationDto dto = new CreateNotificationDto();
        dto.setUserId(message.getUserId());
        dto.setEventId(message.getEventId());
        dto.setMessage("Payment confirmed! Your registration for event #" + message.getEventId()
                + " is now PAID. Amount: " + message.getAmount() + " EGP. Order #" + message.getOrderId());

        notifService.createNotification(dto);

        log.info("[RabbitMQ] PaymentConsumer — Payment confirmation notification created for userId={}",
                message.getUserId());
    }
}
