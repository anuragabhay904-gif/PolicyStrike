
package com.policystrike.api.dto;

import com.policystrike.simulation.AttackType;

import java.math.BigDecimal;
import java.util.List;

public record GeneratedAttackScenariosResponse(
        String policyId,
        int policyVersion,
        String status,
        List<ScenarioSuggestion> scenarios
) {

    public record ScenarioSuggestion(
            AttackType attackType,
            String title,
            String description,
            List<RefundStep> steps
    ) {
    }

    public record RefundStep(
            BigDecimal amount,
            long hoursAfterStart,
            String transactionReference
    ) {
    }
}
