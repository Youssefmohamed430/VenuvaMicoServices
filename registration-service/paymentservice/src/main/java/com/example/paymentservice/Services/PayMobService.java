package com.example.paymentservice.Services;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.paymentservice.Enums.PaymentStatus;
import com.example.paymentservice.Messaging.PaymentPublisher;
import com.example.paymentservice.Messaging.PaymentSuccessEvent;
import com.example.paymentservice.Models.Payment;
import com.example.paymentservice.Repos.PaymentRepo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayMobService {

    private final RestTemplate restTemplate;
    private final PaymentRepo paymentRepository;
    private final PaymentPublisher paymentPublisher; // RabbitMQ publisher

    private static final String BASE_URL = "https://accept.paymob.com/api/";

    // ===== PayMob API Configuration - Injected from Environment Variables =====
    @Value("${PAYMOB_API_KEY:ZXlKaGJHY2lPaUpJVXpVeE1pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SndjbTltYVd4bFgzQnJJam94TVRNd01UZ3NJbU5zWVhOeklqb2lUV1Z5WTJoaGJuUWlMQ0p1WVcxbElqb2lhVzVwZEdsaGJDSjkuazJvdExPbXNZajFQM1FFNTBfeU1mWVBZS0U3S3VuTEpKMThRRkgzR1V0a3dXZG5wNG5kQlc3eDc1WGpOcHNyUTV0ckVPRzZlX2VkdG9jVjJDcHpzc2c=}")
    private String apiKey;

    @Value("${PAYMOB_INTEGRATION_ID:4896849}")
    private int integrationId;

    @Value("${PAYMOB_IFRAME_ID:897502}")
    private int iframeId;

    @Value("${HMAC_SECRET_KEY:0DC8EF3D0DAAB2C53EC6DDD2BEA8EDD0}")
    private String hmacSecretKey;

    // ===== STEP 1: AUTHENTICATE → GET TOKEN =====

    public String authenticate() {
        log.info("[START] PayMobService.authenticate() — Authenticating with PayMob");

        Map<String, String> body = new HashMap<>();
        body.put("api_key", apiKey.trim());

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "auth/tokens",
                body,
                AuthResponse.class
        );

        if (response.getBody() == null || response.getBody().token == null) {
            log.error("[ERROR] PayMobService.authenticate() — PayMob returned null or empty token");
            throw new RuntimeException("PayMob authentication failed: empty token");
        }

        log.info("[OK] PayMobService.authenticate() — Authentication successful");
        return response.getBody().token;
    }

    // ===== STEP 2: CREATE ORDER =====
    
    public int createOrder(String token, int amountCents) {
        log.info("[START] PayMobService.createOrder() — amount_cents: {}", amountCents);

        Map<String, Object> body = new HashMap<>();
        body.put("auth_token", token);
        body.put("delivery_needed", false);
        // `amountCents` is expected to already be in the smallest currency unit (piasters).
        body.put("amount_cents", amountCents * 100);
        body.put("currency", "EGP");
        body.put("items", new Object[0]);

        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                BASE_URL + "ecommerce/orders",
                body,
                OrderResponse.class
        );

        if (response.getBody() == null || response.getBody().id == 0) {
            log.error("[ERROR] PayMobService.createOrder() — PayMob returned invalid order ID");
            throw new RuntimeException("PayMob order creation failed: invalid order ID");
        }

        log.info("[OK] PayMobService.createOrder() — Order created: {}", response.getBody().id);
        return response.getBody().id;
    }

    // ===== STEP 3: GET PAYMENT KEY =====

    public String getPaymentKey(String token, int orderId, int amountCents, int userId, int eventId) {
        log.info("[START] PayMobService.getPaymentKey() — orderId: {}, amount_cents: {}", orderId, amountCents);

        Map<String, Object> billingData = new HashMap<>();
        billingData.put("apartment", String.valueOf(userId));
        billingData.put("email", "customer@example.com");
        billingData.put("floor", String.valueOf(eventId));
        billingData.put("first_name", "Customer");
        billingData.put("street", "NA");
        billingData.put("building", "NA");
        billingData.put("phone_number", "01000000000");
        billingData.put("shipping_method", "NA");
        billingData.put("postal_code", "NA");
        billingData.put("city", "Cairo");
        billingData.put("country", "EG");
        billingData.put("last_name", "NA");
        billingData.put("state", "Cairo");

        Map<String, Object> body = new HashMap<>();
        body.put("auth_token", token);
        // `amountCents` is already in piasters (smallest unit). Do not multiply again.
        body.put("amount_cents", amountCents * 100);
        body.put("expiration", 3600);
        body.put("order_id", orderId);
        body.put("billing_data", billingData);
        body.put("currency", "EGP");
        body.put("integration_id", integrationId);

        ResponseEntity<PaymentKeyResponse> response = restTemplate.postForEntity(
                BASE_URL + "acceptance/payment_keys",
                body,
                PaymentKeyResponse.class
        );

        if (response.getBody() == null || response.getBody().token == null) {
            log.error("[ERROR] PayMobService.getPaymentKey() — PayMob returned invalid payment key");
            throw new RuntimeException("PayMob payment key generation failed");
        }

        log.info("[OK] PayMobService.getPaymentKey() — Payment key generated");
        return response.getBody().token;
    }

    // ===== STEP 4: BUILD IFRAME URL =====

    public String getIframeUrl(String paymentKey) {
        String url = String.format(
                "https://accept.paymob.com/api/acceptance/iframes/%d?payment_token=%s",
                iframeId, paymentKey
        );
        log.info("Generated iframe URL with IFrame ID: {}", iframeId);
        return url;
    }

    // ===== FULL PAYMENT FLOW =====

    public String payWithCard(int amount, int userId, int eventId) {
        log.info("[START] PayMobService.payWithCard() — amount (EGP): {}, userId: {}, eventId: {}", amount, userId, eventId);

        // Convert EGP to piasters (smallest currency unit) once
        int amountCents = amount * 100;

        String token = authenticate();
        int orderId = createOrder(token, amountCents);
        String paymentKey = getPaymentKey(token, orderId, amountCents, userId, eventId);
        String iframeUrl = getIframeUrl(paymentKey);

        // Store a temporary payment record with userId and eventId mapping
        // This will be used in the callback to register the user to the event
        Payment pendingPayment = new Payment();
        // Store the amount in main currency unit (EGP)
        pendingPayment.setAmount(BigDecimal.valueOf(amount));
        pendingPayment.setPaymentStatus(PaymentStatus.PENDING);
        pendingPayment.setTransactionDate(LocalDateTime.now());
        pendingPayment.setOrderId(orderId);
        pendingPayment.setUserId(userId);
        pendingPayment.setEventId(eventId);
        paymentRepository.save(pendingPayment);

        log.info("[OK] PayMobService.payWithCard() — Payment flow completed with orderId: {}", orderId);
        return iframeUrl;
    }

    /**
     * Alias for payWithCard — called by PaymobController.pay()
     */
    public String initiatePayment(int amount, int userId, int eventId) {
        return payWithCard(amount, userId, eventId);
    }

    /**
     * String-based overload for PaymobController.
     * Parses the raw JSON string and delegates to the typed method.
     */
    public boolean paymobCallback(String rawPayload, String hmacHeader) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            PaymobCallbackPayload payload = mapper.readValue(rawPayload, PaymobCallbackPayload.class);
            return paymobCallback(payload, hmacHeader);
        } catch (Exception e) {
            log.error("[ERROR] PayMobService.paymobCallback(String) — Failed to parse payload: {}", e.getMessage());
            return false;
        }
    }

    // ===== CALLBACK VERIFICATION (Typed) =====
    public boolean paymobCallback(PaymobCallbackPayload payload, String hmacHeader) {
        try {
            log.info("[START] PayMobService.paymobCallback() — Processing payment notification");
            
            PaymobObj obj = payload.obj;

            if (obj.data != null && obj.data.message != null) {
                log.warn("[WARN] PayMobService.paymobCallback() — Error Message: {}, TXN Response Code: {}", 
                        obj.data.message, obj.data.txnResponseCode);
            }

            String dataString = obj.amountCents
                    + obj.createdAt
                    + obj.currency
                    + String.valueOf(obj.errorOccurred).toLowerCase()
                    + String.valueOf(obj.hasParentTransaction).toLowerCase()
                    + obj.id
                    + obj.integrationId
                    + String.valueOf(obj.is3dSecure).toLowerCase()
                    + String.valueOf(obj.isAuth).toLowerCase()
                    + String.valueOf(obj.isCapture).toLowerCase()
                    + String.valueOf(obj.isRefunded).toLowerCase()
                    + String.valueOf(obj.isStandalonePayment).toLowerCase()
                    + String.valueOf(obj.isVoided).toLowerCase()
                    + obj.order.id
                    + obj.owner
                    + String.valueOf(obj.pending).toLowerCase()
                    + obj.sourceData.pan
                    + obj.sourceData.subType
                    + obj.sourceData.type
                    + String.valueOf(obj.success).toLowerCase();

            // Compute HMAC-SHA512
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(
                    hmacSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"
            );
            hmac.init(keySpec);
            byte[] hashBytes = hmac.doFinal(dataString.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String calculatedHmac = sb.toString();

            if (!calculatedHmac.equalsIgnoreCase(hmacHeader)) {
                log.warn("[WARN] PayMobService.paymobCallback() — HMAC verification failed for transaction: {}", obj.id);
                return false;
            }

            log.info("[OK] PayMobService.paymobCallback() — HMAC verified for transaction: {}, success: {}", 
                    obj.id, obj.success);

            // ===== RETRIEVE STORED PAYMENT MAPPING =====
            // Use orderId to find the stored payment with userId and eventId
            Payment payment = paymentRepository.findByOrderId(obj.order.id)
                    .orElseThrow(() -> new RuntimeException("Payment record not found for orderId: " + obj.order.id));

            int userId = payment.getUserId();
            int eventId = payment.getEventId();

            log.info("[INFO] PayMobService.paymobCallback() — Retrieved userId: {}, eventId: {} from payment mapping", userId, eventId);

            // ===== UPDATE PAYMENT RECORD =====
            payment.setAmount(BigDecimal.valueOf(obj.amountCents).divide(BigDecimal.valueOf(100)));
            payment.setPaymentStatus(obj.success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            payment.setTransactionDate(LocalDateTime.now());
            paymentRepository.save(payment);

            // ===== PUBLISH RABBITMQ EVENT (Async — Fire-and-Forget) =====
            if (Boolean.TRUE.equals(obj.success)) {
                log.info("[OK] PayMobService.paymobCallback() — Payment successful for transaction: {}", obj.id);
                try {
                    PaymentSuccessEvent event = new PaymentSuccessEvent(
                        userId,
                        eventId,
                        obj.order.id,
                        BigDecimal.valueOf(obj.amountCents).divide(BigDecimal.valueOf(100))
                    );
                    paymentPublisher.publishPaymentSuccess(event);
                } catch (Exception pubEx) {
                    log.warn("[WARN] PayMobService.paymobCallback() — Failed to publish payment.success event: {}", pubEx.getMessage());
                }
            } else {
                log.warn("[WARN] PayMobService.paymobCallback() — Payment unsuccessful for transaction: {}", obj.id);
            }

            return Boolean.TRUE.equals(obj.success);

        } catch (Exception ex) {
            log.error("[ERROR] PayMobService.paymobCallback() — {}", ex.getMessage(), ex);
            return false;
        }
    }

    // ===== INTERNAL RESPONSE DTOs =====

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AuthResponse {
        public String token;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OrderResponse {
        public int id;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PaymentKeyResponse {
        public String token;
    }

    // ===== CALLBACK PAYLOAD DTOs =====

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymobCallbackPayload {
        public String type;
        public PaymobObj obj;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymobObj {
        public Long id;
        public Boolean pending;

        @JsonProperty("amount_cents")
        public Integer amountCents;

        public Boolean success;

        @JsonProperty("is_auth")
        public Boolean isAuth;

        @JsonProperty("is_capture")
        public Boolean isCapture;

        @JsonProperty("is_standalone_payment")
        public Boolean isStandalonePayment;

        @JsonProperty("is_voided")
        public Boolean isVoided;

        @JsonProperty("is_refunded")
        public Boolean isRefunded;

        @JsonProperty("is_3d_secure")
        public Boolean is3dSecure;

        @JsonProperty("integration_id")
        public Integer integrationId;

        @JsonProperty("has_parent_transaction")
        public Boolean hasParentTransaction;

        public PaymobOrder order;

        @JsonProperty("created_at")
        public String createdAt;

        public String currency;

        @JsonProperty("source_data")
        public PaymobSourceData sourceData;

        @JsonProperty("error_occured")
        public Boolean errorOccurred;

        public Integer owner;

        public PaymobData data;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymobOrder {
        public Integer id;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymobSourceData {
        public String type;
        public String pan;

        @JsonProperty("sub_type")
        public String subType;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymobData {
        public String message;

        @JsonProperty("txn_response_code")
        public String txnResponseCode;
    }
}
