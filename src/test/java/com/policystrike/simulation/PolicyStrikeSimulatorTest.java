package com.policystrike.simulation;

import com.policystrike.domain.RefundPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class PolicyStrikeSimulatorTest {

    private final PolicyStrikeSimulator simulator = new PolicyStrikeSimulator();

    @Test
    void shouldGenerate100CustomerProfiles() {
        SimulationReport report = simulator.run();
        assertEquals(100, report.customerCount());
    }

    @Test
    void shouldGenerate1000RefundRequests() {
        SimulationReport report = simulator.run();
        assertEquals(1000, report.totalRequests());
    }

    @Test
    void shouldDetectAllThreeAttackCategoriesForOriginalPolicy() {
        assertAllAttackCategoriesDetected(simulator.run());
    }

    @Test
    void shouldProduceNoFalsePositiveBlocksForOriginalPolicy() {
        assertNoFalsePositivesOrMisses(simulator.run());
    }

    @Test
    void shouldDetectAttacksWithoutFalsePositivesForStricterActivePolicy() {
        RefundPolicy versionTwo = new RefundPolicy(
                "PS-001",
                2,
                new BigDecimal("2000"),
                new BigDecimal("5000"),
                Duration.ofHours(24),
                Duration.ofDays(30),
                true
        );

        SimulationReport report = simulator.run(versionTwo);

        assertEquals(100, report.customerCount());
        assertEquals(1000, report.totalRequests());
        assertEquals(970, report.approvedRequests());
        assertEquals(0, report.manualReviewRequests());
        assertEquals(30, report.blockedRequests());
        assertNoFalsePositivesOrMisses(report);
        assertAllAttackCategoriesDetected(report);
    }

    private void assertNoFalsePositivesOrMisses(SimulationReport report) {
        assertEquals(0, report.falsePositiveBlocks());
        assertEquals(0, report.missedAttacks());
    }

    private void assertAllAttackCategoriesDetected(SimulationReport report) {
        for (AttackType type : new AttackType[]{
                AttackType.THRESHOLD_SPLITTING,
                AttackType.CUMULATIVE_EXPOSURE,
                AttackType.DUPLICATE_REQUEST
        }) {
            assertEquals(
                    report.injectedAttacks().get(type),
                    report.detectedAttacks().get(type),
                    "Not all " + type + " attacks were detected"
            );
        }
    }
}
