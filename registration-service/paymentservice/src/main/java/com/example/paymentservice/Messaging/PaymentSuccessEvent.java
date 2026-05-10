package com.example.paymentservice.Messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Event published when a Paymob payment succeeds.
 * Consumed by:
 *   - Registration Service → updates registration status to PAID
 *   - Notification Service  → sends payment confirmation notification to user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {
    private int userId;
    private int eventId;
    private int orderId;
    private BigDecimal amount;
}
