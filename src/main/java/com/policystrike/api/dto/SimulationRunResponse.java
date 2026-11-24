
package com.policystrike.api.dto;

import com.policystrike.persistence.entity.SimulationRunEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SimulationRunResponse(
        UUID runId,
        String policyId,
        int policyVersion,
        Instant createdAt,
        int customerCount,
        int totalRequests,
        int approvedRequests,
        int manualReviewRequests,
        int blockedRequests,
        BigDecimal approvedExposure,
        int restrictedCustomers,
        int falsePositiveBlocks,
        int missedAttacks,
        List<AttackMetric> attackMetrics
) {

    public record AttackMetric(
            String attackType,
            int injectedCount,
            int detectedCount
    ) {
    }

    public static SimulationRunResponse from(SimulationRunEntity entity) {

        List<AttackMetric> metrics = entity.getAttackResults()
                .stream()
                .map(result -> new AttackMetric(
                        result.getAttackType().name(),
                        result.getInjectedCount(),
                        result.getDetectedCount()
                ))
                .toList();

        return new SimulationRunResponse(
                entity.getId(),
                entity.getPolicyVersion().getPolicyId(),
                entity.getPolicyVersion().getVersion(),
                entity.getCreatedAt(),
                entity.getCustomerCount(),
                entity.getTotalRequests(),
                entity.getApprovedRequests(),
                entity.getManualReviewRequests(),
                entity.getBlockedRequests(),
                entity.getApprovedExposure(),
                entity.getRestrictedCustomers(),
                entity.getFalsePositiveBlocks(),
                entity.getMissedAttacks(),
                metrics
        );
    }
}
