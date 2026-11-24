package com.policystrike.api.dto;

import com.policystrike.domain.DecisionStatus;
import com.policystrike.simulation.AttackType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AttackScenarioExecutionResponse(
        String policyId,
        int policyVersion,
        String status,
        String decisionAuthority,
        Instant simulatedStartAt,
        int scenarioCount,
        int totalRequests,
        int autoApprovedRequests,
        int manualReviewRequests,
        int blockedRequests,
        BigDecimal approvedExposure,
        int mitigatedScenarios,
        int unmitigatedScenarios,
        SimulationRunResponse persistedSimulationRun,
        List<ScenarioResult> scenarios
) {

    public AttackScenarioExecutionResponse {
        scenarios = List.copyOf(scenarios);
    }

    public record ScenarioResult(
            AttackType attackType,
            String title,
            String description,
            int totalRequests,
            int autoApprovedRequests,
            int manualReviewRequests,
            int blockedRequests,
            BigDecimal approvedExposure,
            boolean mitigated,
            List<StepResult> steps
    ) {
        public ScenarioResult {
            steps = List.copyOf(steps);
        }
    }

    public record StepResult(
            String requestId,
            String customerId,
            String transactionReference,
            BigDecimal amount,
            Instant requestedAt,
            DecisionStatus decisionStatus,
            String decisionReason
    ) {
    }
}
