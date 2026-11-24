
package com.policystrike.api.dto;

import com.policystrike.domain.DecisionStatus;
import com.policystrike.persistence.entity.RefundAuditEntity;
import com.policystrike.simulation.AttackType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundAuditResponse(
        UUID auditId,
        UUID simulationRunId,
        String policyId,
        int policyVersion,
        String requestId,
        String customerId,
        String transactionId,
        BigDecimal amount,
        Instant requestedAt,
        Instant evaluatedAt,
        DecisionStatus decisionStatus,
        String decisionReason,
        AttackType scenarioLabel
) {

    public static RefundAuditResponse from(RefundAuditEntity entity) {

        return new RefundAuditResponse(
                entity.getId(),
                entity.getSimulationRun().getId(),
                entity.getPolicyVersion().getPolicyId(),
                entity.getPolicyVersion().getVersion(),
                entity.getRequestId(),
                entity.getCustomerId(),
                entity.getTransactionId(),
                entity.getAmount(),
                entity.getRequestedAt(),
                entity.getEvaluatedAt(),
                entity.getDecisionStatus(),
                entity.getDecisionReason(),
                entity.getScenarioLabel()
        );
    }
}
