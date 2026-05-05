package com.example.paymentservice.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/Paymob")
@RequiredArgsConstructor
@Slf4j
public class PaymobController {

    // ===== POST /api/Paymob/pay =====
    // Initiate a card payment — returns a PayMob iFrame URL
    // userId is passed as a query param (or extracted from JWT in production)
    @PostMapping("/pay")
    public ResponseEntity<?> pay(
            @RequestParam(required = false) Integer amount,
            @RequestParam(required = false) Integer amountCents,
            @RequestParam int userId,
            @RequestParam int eventId) {
        try {
            // Accept either `amount` (EGP) or `amountCents` (smallest unit). Prefer `amount` when provided.
            int amountToUse;
            if (amount != null) {
                amountToUse = amount;
            } else if (amountCents != null) {
                // convert piasters to EGP
                amountToUse = amountCents / 100;
            } else {
                throw new IllegalArgumentException("Required request parameter 'amount' or 'amountCents' is missing");
            }

            log.info("PaymobController.pay() called with userId={}, amount(EGP)={}", userId, amountToUse);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body("PayMob integration is not implemented in this service module.");
        } catch (Exception ex) {
            // Log full exception details for diagnosis
            log.error("PAYMENT FAILED - Cause: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Payment failed: " + ex.getMessage());
        }
    }

    // ===== POST /api/Paymob/callback =====
    // PayMob webhook — receives transaction result and verifies HMAC
    // Must be public (no auth) so PayMob servers can call it
    @PostMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestBody Object payload,
            @RequestParam(value = "hmac", required = false) String hmacHeader) {
        try {
            log.info("PaymobController.callback() received PayMob notification");
            // Always return 200 to PayMob so it doesn't retry
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("PaymobController.callback() error processing PayMob callback", ex);
            // Still return 200 to prevent PayMob retry loops
            return ResponseEntity.ok().build();
        }
    }
}

