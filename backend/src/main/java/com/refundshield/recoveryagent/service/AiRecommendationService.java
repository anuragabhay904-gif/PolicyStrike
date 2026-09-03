package com.refundshield.recoveryagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refundshield.recoveryagent.dto.AiAnalysisDTO;
import com.refundshield.recoveryagent.entity.Customer;
import com.refundshield.recoveryagent.entity.Opportunity;
import com.refundshield.recoveryagent.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(AiRecommendationService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    public AiRecommendationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public AiAnalysisDTO generateRecommendation(Opportunity opportunity, Customer customer, Transaction transaction, List<Transaction> history) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equalsIgnoreCase("demo")) {
            logger.info("Gemini API key not configured or in demo mode. Using deterministic fallback.");
            return createFallback(opportunity, customer, transaction);
        }

        try {
            String prompt = buildPrompt(opportunity, customer, transaction, history);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey;

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(textPart));
            Map<String, Object> generationConfig = Map.of("responseMimeType", "application/json");

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(content),
                    "generationConfig", generationConfig
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String jsonText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                    return objectMapper.readValue(jsonText, AiAnalysisDTO.class);
                }
            }
        } catch (Exception ex) {
            logger.warn("Failed to call Gemini API ({}: {}). Falling back gracefully.", ex.getClass().getSimpleName(), ex.getMessage());
        }

        return createFallback(opportunity, customer, transaction);
    }

    private String buildPrompt(Opportunity opportunity, Customer customer, Transaction transaction, List<Transaction> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI Revenue Recovery specialist for a merchant platform.\n");
        sb.append("Analyze the provided customer and transaction data to explain why revenue is at risk and recommend a recovery strategy.\n\n");
        sb.append("CRITICAL INSTRUCTIONS:\n");
        sb.append("- Do not invent customer information.\n");
        sb.append("- Do not invent transaction information.\n");
        sb.append("- Do not authorize financial transactions.\n");
        sb.append("- Do not modify recovery amounts.\n");
        sb.append("- Do not make payment decisions.\n");
        sb.append("- Only explain and recommend based on supplied data.\n");
        sb.append("- Output must be strictly valid JSON matching this schema:\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"Brief 1-2 sentence executive summary\",\n");
        sb.append("  \"reason\": \"Detailed analysis of why this revenue is lost/at risk\",\n");
        sb.append("  \"recommendation\": \"Specific operational action for the merchant\",\n");
        sb.append("  \"message\": \"Polite, ready-to-send customer outreach message including link placeholder\",\n");
        sb.append("  \"confidence\": \"HIGH\" | \"MEDIUM\" | \"LOW\"\n");
        sb.append("}\n\n");

        sb.append("DATA:\n");
        sb.append("Opportunity Type: ").append(opportunity.getType()).append("\n");
        sb.append("Recovery Amount: ₹").append(opportunity.getAmount()).append("\n");
        sb.append("Deterministic Recovery Score: ").append(opportunity.getRecoveryScore()).append("/100\n");

        if (customer != null) {
            sb.append("Customer Name: ").append(customer.getName()).append("\n");
            sb.append("Customer Email: ").append(customer.getEmail()).append("\n");
            sb.append("Total Historical Orders: ").append(customer.getTotalOrders()).append("\n");
            sb.append("Total Historical Spent: ₹").append(customer.getTotalSpent()).append("\n");
            sb.append("Last Order Date: ").append(customer.getLastOrderDate()).append("\n");
        }

        if (transaction != null) {
            sb.append("Transaction ID: ").append(transaction.getId()).append("\n");
            sb.append("Transaction Status: ").append(transaction.getStatus()).append("\n");
            sb.append("Payment Method/Channel: ").append(transaction.getTransactionType()).append("\n");
            sb.append("Transaction Timestamp: ").append(transaction.getTransactionDate()).append("\n");
        }

        if (history != null && !history.isEmpty()) {
            sb.append("Recent Transaction History: ").append(history.size()).append(" past records.\n");
        }

        return sb.toString();
    }

    private AiAnalysisDTO createFallback(Opportunity opportunity, Customer customer, Transaction transaction) {
        String customerName = (customer != null && customer.getName() != null) ? customer.getName() : "Valued Customer";
        String amountStr = (opportunity.getAmount() != null) ? "₹" + opportunity.getAmount().toPlainString() : "your order amount";

        switch (opportunity.getType()) {
            case "FAILED_PAYMENT_RECOVERY" -> {
                String method = (transaction != null && transaction.getTransactionType() != null) ? transaction.getTransactionType() : "card/UPI";
                return new AiAnalysisDTO(
                        "Recent payment attempt of " + amountStr + " failed due to payment gateway/network interruption.",
                        "The customer attempted to complete an order of " + amountStr + " via " + method + ", but the transaction failed. Since this customer has proven purchase intent, generating an active Razorpay recovery link will capture the dropped revenue before cart abandonment becomes permanent.",
                        "Approve and issue a direct Razorpay Test payment link with SMS/Email outreach within 2 hours.",
                        "Hi " + customerName + ", we noticed your recent payment of " + amountStr + " couldn't be completed. You can safely complete your order using this secure link: {{PAYMENT_LINK}}",
                        "HIGH"
                );
            }
            case "HIGH_VALUE_CHECKOUT_RECOVERY" -> {
                return new AiAnalysisDTO(
                        "High-value order of " + amountStr + " abandoned before final authorization.",
                        "The transaction value of " + amountStr + " significantly exceeds standard cart averages. Abandonment at this price point represents notable margin leakage. Customer showed active intent but abandoned before completing authorization.",
                        "Send a prioritized payment link with a personalized assistance note.",
                        "Hi " + customerName + ", your cart with items worth " + amountStr + " is reserved for you. Complete your checkout securely here: {{PAYMENT_LINK}}",
                        "HIGH"
                );
            }
            case "REPEAT_CUSTOMER_REENGAGEMENT" -> {
                return new AiAnalysisDTO(
                        "High-value repeat customer dormant beyond typical reorder threshold.",
                        "Customer " + customerName + " has contributed strong historical lifetime revenue but has not transacted in over 30 days. Re-engaging with an exclusive re-order link or curated recommendation protects lifetime customer value.",
                        "Send a re-engagement notification with exclusive re-order payment link.",
                        "Hi " + customerName + ", we miss having you! As one of our top customers, we've prepared a special restock offer of " + amountStr + " for you: {{PAYMENT_LINK}}",
                        "MEDIUM"
                );
            }
            default -> {
                return new AiAnalysisDTO(
                        "Potential revenue recovery opportunity detected.",
                        "A payment or customer activity pattern indicates possible recoverable revenue.",
                        "Review the opportunity and contact the customer.",
                        "Please complete your payment to continue your order: {{PAYMENT_LINK}}",
                        "MEDIUM"
                );
            }
        }
    }
}
