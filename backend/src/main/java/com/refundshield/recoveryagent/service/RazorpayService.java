package com.refundshield.recoveryagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refundshield.recoveryagent.entity.Customer;
import com.refundshield.recoveryagent.entity.Opportunity;
import com.refundshield.recoveryagent.exception.PaymentGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RazorpayService {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.mode:DEMO}")
    private String paymentMode;

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    // Toggle for controlled failure demonstration (Section 15)
    private final AtomicBoolean simulateFailure = new AtomicBoolean(false);

    public RazorpayService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public PaymentLinkResult createPaymentLink(Opportunity opportunity, Customer customer) {
        // Check failure simulation scenario first
        if (simulateFailure.get()) {
            logger.warn("Controlled failure simulation active. Rejecting payment link creation.");
            throw new PaymentGatewayException("Razorpay Gateway Error [SIMULATED_FAILURE]: 503 Service Unavailable / Gateway Timeout. No customer charge was completed.");
        }

        boolean isRealRazorpay = "RAZORPAY".equalsIgnoreCase(paymentMode) &&
                keyId != null && !keyId.trim().isEmpty() &&
                keySecret != null && !keySecret.trim().isEmpty();

        if (!isRealRazorpay) {
            logger.info("Operating in DEMO payment mode. Generating simulated Razorpay test payment link.");
            String shortCode = UUID.randomUUID().toString().substring(0, 8);
            String refCode = UUID.randomUUID().toString().substring(0, 12);
            return new PaymentLinkResult(
                    "https://rzp.io/i/demo_" + shortCode,
                    "plink_demo_" + refCode,
                    "CREATED"
            );
        }

        try {
            String url = "https://api.razorpay.com/v1/payment_links";

            long amountInPaise = opportunity.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, Object> customerData = new HashMap<>();
            customerData.put("name", customer != null ? customer.getName() : "Customer");
            customerData.put("email", customer != null ? customer.getEmail() : "customer@example.com");
            if (customer != null && customer.getPhone() != null) {
                customerData.put("contact", customer.getPhone());
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", amountInPaise);
            payload.put("currency", "INR");
            payload.put("accept_partial", false);
            payload.put("description", "Payment recovery for Opportunity #" + opportunity.getId() + " (" + opportunity.getType() + ")");
            payload.put("customer", customerData);
            payload.put("notify", Map.of("sms", false, "email", false));
            payload.put("reminder_enable", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String auth = keyId + ":" + keySecret;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + new String(encodedAuth));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String shortUrl = root.path("short_url").asText();
                String paymentLinkId = root.path("id").asText();
                String status = root.path("status").asText("created").toUpperCase();

                logger.info("Successfully created Razorpay Test payment link: {} ({})", shortUrl, paymentLinkId);
                return new PaymentLinkResult(shortUrl, paymentLinkId, status);
            } else {
                throw new PaymentGatewayException("Razorpay API returned status: " + response.getStatusCode());
            }
        } catch (PaymentGatewayException pge) {
            throw pge;
        } catch (Exception ex) {
            logger.error("Error communicating with Razorpay API", ex);
            throw new PaymentGatewayException("Razorpay Test API Error: " + ex.getMessage(), ex);
        }
    }

    public boolean isSimulateFailure() {
        return simulateFailure.get();
    }

    public void setSimulateFailure(boolean fail) {
        simulateFailure.set(fail);
        logger.info("Simulate Razorpay failure set to: {}", fail);
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public static class PaymentLinkResult {
        private final String paymentLink;
        private final String razorpayReference;
        private final String status;

        public PaymentLinkResult(String paymentLink, String razorpayReference, String status) {
            this.paymentLink = paymentLink;
            this.razorpayReference = razorpayReference;
            this.status = status;
        }

        public String getPaymentLink() { return paymentLink; }
        public String getRazorpayReference() { return razorpayReference; }
        public String getStatus() { return status; }
    }
}
