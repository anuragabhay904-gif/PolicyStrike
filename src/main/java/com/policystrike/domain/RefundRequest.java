package com.policystrike.domain;

import java.math.BigDecimal;
import java.time.Instant;

public final class RefundRequest {

    private final String requestId;
    private final String customerId;
    private final String transactionId;
    private final BigDecimal amount;
    private final Instant requestedAt;

    public RefundRequest(
            String requestId,
            String customerId,
            String transactionId,
            BigDecimal amount,
            Instant requestedAt) {

        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Request ID is required");
        }

        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }

        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }

        if (requestedAt == null) {
            throw new IllegalArgumentException("Request timestamp is required");
        }

        this.requestId = requestId;
        this.customerId = customerId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.requestedAt = requestedAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}
