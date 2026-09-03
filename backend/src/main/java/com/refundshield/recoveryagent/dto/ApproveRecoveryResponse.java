package com.refundshield.recoveryagent.dto;

import java.math.BigDecimal;

public class ApproveRecoveryResponse {
    private boolean success;
    private String message;
    private Long opportunityId;
    private Long recoveryActionId;
    private BigDecimal amount;
    private String paymentLink;
    private String razorpayReference;
    private String status;

    public ApproveRecoveryResponse() {}

    public ApproveRecoveryResponse(boolean success, String message, Long opportunityId, Long recoveryActionId, BigDecimal amount, String paymentLink, String razorpayReference, String status) {
        this.success = success;
        this.message = message;
        this.opportunityId = opportunityId;
        this.recoveryActionId = recoveryActionId;
        this.amount = amount;
        this.paymentLink = paymentLink;
        this.razorpayReference = razorpayReference;
        this.status = status;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getOpportunityId() { return opportunityId; }
    public void setOpportunityId(Long opportunityId) { this.opportunityId = opportunityId; }

    public Long getRecoveryActionId() { return recoveryActionId; }
    public void setRecoveryActionId(Long recoveryActionId) { this.recoveryActionId = recoveryActionId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentLink() { return paymentLink; }
    public void setPaymentLink(String paymentLink) { this.paymentLink = paymentLink; }

    public String getRazorpayReference() { return razorpayReference; }
    public void setRazorpayReference(String razorpayReference) { this.razorpayReference = razorpayReference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
