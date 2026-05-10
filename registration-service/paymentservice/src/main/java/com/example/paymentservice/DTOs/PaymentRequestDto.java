package com.example.paymentservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    @NotNull
    private int userId;
    @NotNull
    private int eventId;
    @NotNull
    private int amount; // Amount in EGP
}
