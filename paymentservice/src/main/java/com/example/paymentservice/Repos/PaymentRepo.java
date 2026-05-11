package com.example.paymentservice.Repos;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.paymentservice.Models.Payment;
import java.util.Optional;

public interface PaymentRepo extends JpaRepository<Payment, Integer> {
    
    /**
     * Find a payment record by PayMob order ID
     * Used to correlate PayMob webhooks with local payment records
     */
    Optional<Payment> findByOrderId(int orderId);
}
