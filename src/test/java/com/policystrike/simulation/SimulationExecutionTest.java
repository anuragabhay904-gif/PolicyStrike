
package com.policystrike.simulation;

import com.policystrike.domain.RefundPolicy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SimulationExecutionTest {

    private final PolicyStrikeSimulator simulator =
            new PolicyStrikeSimulator();

    private RefundPolicy createPolicy() {
        return new RefundPolicy(
                "PS-001",
                1,
                new BigDecimal("7500"),
                new BigDecimal("20000"),
                Duration.ofHours(24),
                Duration.ofDays(30),
                true
        );
    }

    @Test
    void shouldPreserveOneEvaluationForEveryRequest() {

        SimulationExecution execution =
                simulator.runDetailed(createPolicy());

        assertEquals(
                execution.report().totalRequests(),
                execution.evaluations().size()
        );

        assertEquals(1000, execution.evaluations().size());
    }

    @Test
    void shouldPreserveAllThreeAttackLabels() {

        SimulationExecution execution =
                simulator.runDetailed(createPolicy());

        long thresholdAttacks = execution.evaluations()
                .stream()
                .filter(record ->
                        record.scenarioLabel() ==
                                AttackType.THRESHOLD_SPLITTING)
                .count();

        long cumulativeAttacks = execution.evaluations()
                .stream()
                .filter(record ->
                        record.scenarioLabel() ==
                                AttackType.CUMULATIVE_EXPOSURE)
                .count();

        long duplicateAttacks = execution.evaluations()
                .stream()
                .filter(record ->
                        record.scenarioLabel() ==
                                AttackType.DUPLICATE_REQUEST)
                .count();

        assertEquals(10, thresholdAttacks);
        assertEquals(10, cumulativeAttacks);
        assertEquals(10, duplicateAttacks);
    }
}
