
package com.policystrike.simulation;

import com.policystrike.domain.RefundDecision;
import com.policystrike.domain.RefundRequest;

import java.util.Objects;

public record EvaluationRecord(
        RefundRequest request,
        RefundDecision decision,
        AttackType scenarioLabel) {

    public EvaluationRecord {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(decision, "Decision is required");
        Objects.requireNonNull(scenarioLabel, "Scenario label is required");
    }
}
