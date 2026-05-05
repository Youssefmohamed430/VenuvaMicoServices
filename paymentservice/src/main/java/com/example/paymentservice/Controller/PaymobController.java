package com.example.paymentservice.Controller;

import com.example.AOP.Annotation.HandleException;
import com.example.AOP.Annotation.Loggable;
import com.example.paymentservice.DTOs.PaymentRequestDto;
import com.example.paymentservice.Services.PayMobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/Paymob")
@RequiredArgsConstructor
@Slf4j
public class PaymobController {

    private final PayMobService payMobService;

    /**
     * POST /api/Paymob/pay
     * Initiate a card payment — returns a PayMob iFrame URL.
     * userId is extracted from JWT in production (passed as param during development).
     */
    @PostMapping("/pay")
    @HandleException
    @Loggable(value = "InitiatePayment", logArguments = false, logResult = false)
    public ResponseEntity<?> pay(
            @RequestParam(required = false) Integer amount,
            @RequestParam(required = false) Integer amountCents,
            @RequestParam int userId,
            @RequestParam int eventId) {

        // Accept either `amount` (EGP) or `amountCents` (smallest unit)
        int amountToUse;
        if (amount != null) {
            amountToUse = amount;
        } else if (amountCents != null) {
            amountToUse = amountCents / 100;
        } else {
            throw new IllegalArgumentException("Required request parameter 'amount' or 'amountCents' is missing");
        }

        log.info("[USER] PaymobController.pay() — userId={}, amount(EGP)={}", userId, amountToUse);
        String iframeUrl = payMobService.initiatePayment(amountToUse, userId, eventId);
        return ResponseEntity.ok(Map.of("iframeUrl", iframeUrl));
    }

    /**
     * POST /api/Paymob/callback
     * PayMob webhook — receives transaction result and verifies HMAC.
     * Must be PUBLIC (no auth) — PayMob servers call this directly.
     * MUST always return 200 OK immediately; actual processing is async (RabbitMQ).
     */
    @PostMapping("/callback")
    @HandleException
    @Loggable(value = "PaymobCallback", logArguments = false, logResult = false)
    public ResponseEntity<Void> callback(
            @RequestBody String payload,
            @RequestParam(value = "hmac", required = false) String hmacHeader) {

        log.info("[WEBHOOK] PaymobController.callback() — PayMob notification received, hmac={}", hmacHeader);

        try {
            payMobService.paymobCallback(payload, hmacHeader);
        } catch (Exception ex) {
            // Log but don't fail — always return 200 so PayMob doesn't retry
            log.error("[WEBHOOK] PaymobController.callback() — Error processing callback: {}", ex.getMessage(), ex);
        }

        // Always return 200 to PayMob — processing is async via RabbitMQ
        return ResponseEntity.ok().build();
    }
}
