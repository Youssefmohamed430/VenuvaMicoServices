package com.example.registration_service.Messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published by Payment Service when a payment succeeds.
 * Registration Service listens for this event to update registration status to PAID.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {
    private int userId;
    private int eventId;
    private int orderId;
    private java.math.BigDecimal amount;
}
