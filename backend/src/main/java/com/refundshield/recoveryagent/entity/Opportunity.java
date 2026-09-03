package com.refundshield.recoveryagent.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "opportunities")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    private Long transactionId;

    @Column(nullable = false)
    private String type; // FAILED_PAYMENT_RECOVERY, HIGH_VALUE_CHECKOUT_RECOVERY, REPEAT_CUSTOMER_REENGAGEMENT

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer recoveryScore; // 0 - 100

    @Column(nullable = false)
    private String status; // NEW, REVIEWED, APPROVED, DISMISSED, RECOVERY_CREATED, RECOVERED, FAILED

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String aiExplanation;

    @Column(columnDefinition = "TEXT")
    private String aiRecommendation;

    @Column(columnDefinition = "TEXT")
    private String suggestedMessage;

    private String confidence; // HIGH, MEDIUM, LOW

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public Opportunity() {}

    public Opportunity(Long customerId, Long transactionId, String type, BigDecimal amount, Integer recoveryScore, String status, String reason) {
        this.customerId = customerId;
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.recoveryScore = recoveryScore;
        this.status = status;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getRecoveryScore() { return recoveryScore; }
    public void setRecoveryScore(Integer recoveryScore) { this.recoveryScore = recoveryScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }

    public String getAiRecommendation() { return aiRecommendation; }
    public void setAiRecommendation(String aiRecommendation) { this.aiRecommendation = aiRecommendation; }

    public String getSuggestedMessage() { return suggestedMessage; }
    public void setSuggestedMessage(String suggestedMessage) { this.suggestedMessage = suggestedMessage; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
