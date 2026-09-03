package com.refundshield.recoveryagent.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_actions")
public class RecoveryAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long opportunityId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String actionType; // PAYMENT_LINK, EMAIL_DISCOUNT, OUTREACH

    @Column(nullable = false)
    private String status; // CREATED, FAILED, PAID, EXPIRED

    @Column(columnDefinition = "TEXT")
    private String paymentLink;

    private String razorpayReference;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private LocalDateTime createdAt = LocalDateTime.now();

    public RecoveryAction() {}

    public RecoveryAction(Long opportunityId, Long customerId, BigDecimal amount, String actionType, String status, String paymentLink, String razorpayReference) {
        this.opportunityId = opportunityId;
        this.customerId = customerId;
        this.amount = amount;
        this.actionType = actionType;
        this.status = status;
        this.paymentLink = paymentLink;
        this.razorpayReference = razorpayReference;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOpportunityId() { return opportunityId; }
    public void setOpportunityId(Long opportunityId) { this.opportunityId = opportunityId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentLink() { return paymentLink; }
    public void setPaymentLink(String paymentLink) { this.paymentLink = paymentLink; }

    public String getRazorpayReference() { return razorpayReference; }
    public void setRazorpayReference(String razorpayReference) { this.razorpayReference = razorpayReference; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
