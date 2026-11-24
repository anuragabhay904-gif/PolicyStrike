package com.policystrike.simulation;

import java.math.BigDecimal;
import java.util.Map;

public record SimulationReport(
        int customerCount,
        int totalRequests,
        int approvedRequests,
        int manualReviewRequests,
        int blockedRequests,
        BigDecimal approvedExposure,
        int restrictedCustomers,
        Map<AttackType, Integer> injectedAttacks,
        Map<AttackType, Integer> detectedAttacks,
        int falsePositiveBlocks,
        int missedAttacks) {

    public SimulationReport {
        injectedAttacks = Map.copyOf(injectedAttacks);
        detectedAttacks = Map.copyOf(detectedAttacks);
    }
}
