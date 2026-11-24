package com.policystrike.domain;

import java.util.Objects;

public final class RefundDecision {

    private final String requestId;
    private final DecisionStatus status;
    private final String reason;

    public RefundDecision(
            String requestId,
            DecisionStatus status,
            String reason) {

        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Request ID is required");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Decision reason is required");
        }

        this.requestId = requestId;
        this.status = Objects.requireNonNull(status, "Status is required");
        this.reason = reason;
    }

    public String getRequestId() {
        return requestId;
    }

    public DecisionStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
