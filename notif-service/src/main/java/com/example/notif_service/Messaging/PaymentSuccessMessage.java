package com.example.notif_service.Messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/** Received when Payment Service publishes a successful payment */
@Data @NoArgsConstructor @AllArgsConstructor
public class PaymentSuccessMessage {
    private int userId;
    private int eventId;
    private int orderId;
    private BigDecimal amount;
}
