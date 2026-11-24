package com.policystrike.api.dto;

import com.policystrike.persistence.entity.PolicyVersionEntity;
import com.policystrike.persistence.entity.PolicyVersionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PolicyVersionResponse(
        String policyId,
        int version,
        BigDecimal automaticRefundLimit,
        BigDecimal customerCumulativeLimit,
        long thresholdSplittingWindowSeconds,
        long cumulativeExposureWindowSeconds,
        boolean duplicateProtectionEnabled,
        PolicyVersionStatus status,
        Instant createdAt,
        String approvedBy,
        Instant approvedAt,
        String rejectedBy,
        Instant rejectedAt,
        String rejectionReason
) {
    public static PolicyVersionResponse from(PolicyVersionEntity entity) {
        return new PolicyVersionResponse(
                entity.getPolicyId(),
                entity.getVersion(),
                entity.getAutomaticRefundLimit(),
                entity.getCustomerCumulativeLimit(),
                entity.getThresholdSplittingWindowSeconds(),
                entity.getCumulativeExposureWindowSeconds(),
                entity.isDuplicateProtectionEnabled(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectedBy(),
                entity.getRejectedAt(),
                entity.getRejectionReason()
        );
    }
}
