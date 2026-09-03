package com.refundshield.recoveryagent.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OpportunityDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Integer customerTotalOrders;
    private BigDecimal customerTotalSpent;

    private Long transactionId;
    private String transactionStatus;
    private String transactionType;
    private LocalDateTime transactionDate;

    private String type;
    private BigDecimal amount;
    private Integer recoveryScore;
    private String status;
    private String reason;

    private String aiExplanation;
    private String aiRecommendation;
    private String suggestedMessage;
    private String confidence;

    private Long recoveryActionId;
    private String recoveryActionStatus;
    private String paymentLink;
    private String razorpayReference;
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OpportunityDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public Integer getCustomerTotalOrders() { return customerTotalOrders; }
    public void setCustomerTotalOrders(Integer customerTotalOrders) { this.customerTotalOrders = customerTotalOrders; }

    public BigDecimal getCustomerTotalSpent() { return customerTotalSpent; }
    public void setCustomerTotalSpent(BigDecimal customerTotalSpent) { this.customerTotalSpent = customerTotalSpent; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

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

    public Long getRecoveryActionId() { return recoveryActionId; }
    public void setRecoveryActionId(Long recoveryActionId) { this.recoveryActionId = recoveryActionId; }

    public String getRecoveryActionStatus() { return recoveryActionStatus; }
    public void setRecoveryActionStatus(String recoveryActionStatus) { this.recoveryActionStatus = recoveryActionStatus; }

    public String getPaymentLink() { return paymentLink; }
    public void setPaymentLink(String paymentLink) { this.paymentLink = paymentLink; }

    public String getRazorpayReference() { return razorpayReference; }
    public void setRazorpayReference(String razorpayReference) { this.razorpayReference = razorpayReference; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
