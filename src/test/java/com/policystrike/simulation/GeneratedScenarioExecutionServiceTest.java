package com.policystrike.simulation;

import com.policystrike.api.dto.AttackScenarioExecutionResponse;
import com.policystrike.api.dto.GeneratedAttackScenariosResponse;
import com.policystrike.api.dto.SimulationRunResponse;
import com.policystrike.domain.RefundPolicy;
import com.policystrike.persistence.entity.PolicyVersionEntity;
import com.policystrike.persistence.service.SimulationHistoryService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratedScenarioExecutionServiceTest {

    @Mock
    private SimulationHistoryService historyService;

    @InjectMocks
    private GeneratedScenarioExecutionService executionService;

    @Test
    void executesAllThreeAttackTypesAndPersistsEveryDecision() {
        RefundPolicy policy = defaultPolicy();
        PolicyVersionEntity policyEntity = PolicyVersionEntity.from(policy);
        GeneratedAttackScenariosResponse generated = generatedScenarios();
        SimulationRunResponse persistedRun = persistedRun();

        when(historyService.saveRun(any(), any())).thenReturn(persistedRun);

        AttackScenarioExecutionResponse response =
                executionService.execute(policyEntity, generated);

        assertEquals("PS-001", response.policyId());
        assertEquals(1, response.policyVersion());
        assertEquals("EXECUTED", response.status());
        assertEquals("DETERMINISTIC_JAVA_POLICY_ENGINE", response.decisionAuthority());
        assertEquals(3, response.scenarioCount());
        assertEquals(10, response.totalRequests());
        assertEquals(5, response.autoApprovedRequests());
        assertEquals(0, response.manualReviewRequests());
        assertEquals(5, response.blockedRequests());
        assertEquals(new BigDecimal("27000"), response.approvedExposure());
        assertEquals(3, response.mitigatedScenarios());
        assertEquals(0, response.unmitigatedScenarios());
        assertEquals(persistedRun, response.persistedSimulationRun());

        assertEquals(3, response.scenarios().size());
        assertTrue(response.scenarios().stream()
                .allMatch(AttackScenarioExecutionResponse.ScenarioResult::mitigated));
        assertEquals(
                response.scenarios().get(0).steps().get(0).customerId(),
                response.scenarios().get(0).steps().get(1).customerId()
        );
        assertNotEquals(
                response.scenarios().get(0).steps().get(0).customerId(),
                response.scenarios().get(1).steps().get(0).customerId()
        );

        AttackScenarioExecutionResponse.ScenarioResult threshold =
                response.scenarios().get(0);
        assertEquals(1, threshold.autoApprovedRequests());
        assertEquals(2, threshold.blockedRequests());

        AttackScenarioExecutionResponse.ScenarioResult cumulative =
                response.scenarios().get(1);
        assertEquals(3, cumulative.autoApprovedRequests());
        assertEquals(1, cumulative.blockedRequests());

        AttackScenarioExecutionResponse.ScenarioResult duplicate =
                response.scenarios().get(2);
        assertEquals(1, duplicate.autoApprovedRequests());
        assertEquals(2, duplicate.blockedRequests());
        assertEquals(
                duplicate.steps().get(0).transactionReference(),
                duplicate.steps().get(1).transactionReference()
        );
        assertEquals(
                duplicate.steps().get(1).transactionReference(),
                duplicate.steps().get(2).transactionReference()
        );

        ArgumentCaptor<SimulationExecution> executionCaptor =
                ArgumentCaptor.forClass(SimulationExecution.class);
        verify(historyService).saveRun(any(PolicyVersionEntity.class), executionCaptor.capture());

        SimulationExecution persistedExecution = executionCaptor.getValue();
        assertEquals(10, persistedExecution.evaluations().size());
        assertEquals(3, persistedExecution.report().customerCount());
        assertEquals(10, persistedExecution.report().totalRequests());
        assertEquals(5, persistedExecution.report().approvedRequests());
        assertEquals(5, persistedExecution.report().blockedRequests());
        assertEquals(0, persistedExecution.report().missedAttacks());
        assertEquals(Integer.valueOf(1), persistedExecution.report().injectedAttacks()
                .get(AttackType.THRESHOLD_SPLITTING));
        assertEquals(Integer.valueOf(1), persistedExecution.report().injectedAttacks()
                .get(AttackType.CUMULATIVE_EXPOSURE));
        assertEquals(Integer.valueOf(1), persistedExecution.report().injectedAttacks()
                .get(AttackType.DUPLICATE_REQUEST));
        assertEquals(Integer.valueOf(1), persistedExecution.report().detectedAttacks()
                .get(AttackType.THRESHOLD_SPLITTING));
        assertEquals(Integer.valueOf(1), persistedExecution.report().detectedAttacks()
                .get(AttackType.CUMULATIVE_EXPOSURE));
        assertEquals(Integer.valueOf(1), persistedExecution.report().detectedAttacks()
                .get(AttackType.DUPLICATE_REQUEST));
    }

    @Test
    void rejectsScenariosGeneratedForAnOlderPolicyVersion() {
        PolicyVersionEntity currentPolicy =
                PolicyVersionEntity.from(defaultPolicy());

        GeneratedAttackScenariosResponse stale = new GeneratedAttackScenariosResponse(
                "PS-001",
                2,
                "SUGGESTED_NOT_EXECUTED",
                generatedScenarios().scenarios()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> executionService.execute(currentPolicy, stale)
        );

        assertEquals(409, exception.getStatusCode().value());
        verifyNoInteractions(historyService);
    }

    @Test
    void rejectsThresholdScenarioThatDoesNotExceedItsPolicyWindowLimit() {
        PolicyVersionEntity policyEntity =
                PolicyVersionEntity.from(defaultPolicy());

        GeneratedAttackScenariosResponse invalid = new GeneratedAttackScenariosResponse(
                "PS-001",
                1,
                "SUGGESTED_NOT_EXECUTED",
                List.of(new GeneratedAttackScenariosResponse.ScenarioSuggestion(
                        AttackType.THRESHOLD_SPLITTING,
                        "Weak threshold test",
                        "The requests do not exceed the policy threshold.",
                        List.of(
                                step("1000", 0, "TX-1"),
                                step("1000", 1, "TX-2")
                        )
                ))
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> executionService.execute(policyEntity, invalid)
        );

        assertEquals(422, exception.getStatusCode().value());
        verifyNoInteractions(historyService);
    }

    private static RefundPolicy defaultPolicy() {
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

    private static GeneratedAttackScenariosResponse generatedScenarios() {
        return new GeneratedAttackScenariosResponse(
                "PS-001",
                1,
                "SUGGESTED_NOT_EXECUTED",
                List.of(
                        new GeneratedAttackScenariosResponse.ScenarioSuggestion(
                                AttackType.THRESHOLD_SPLITTING,
                                "Automated Refund Limit Evasion via Splitting",
                                "Multiple refunds exceed the rolling automatic refund limit.",
                                List.of(
                                        step("7000", 0, "TXN-SPLIT-001"),
                                        step("7000", 6, "TXN-SPLIT-002"),
                                        step("6500", 12, "TXN-SPLIT-003")
                                )
                        ),
                        new GeneratedAttackScenariosResponse.ScenarioSuggestion(
                                AttackType.CUMULATIVE_EXPOSURE,
                                "Long-Term Cumulative Limit Exhaustion",
                                "Small requests exceed the cumulative customer limit over 30 days.",
                                List.of(
                                        step("5000", 0, "TXN-CUM-001"),
                                        step("5000", 168, "TXN-CUM-002"),
                                        step("5000", 336, "TXN-CUM-003"),
                                        step("6000", 504, "TXN-CUM-004")
                                )
                        ),
                        new GeneratedAttackScenariosResponse.ScenarioSuggestion(
                                AttackType.DUPLICATE_REQUEST,
                                "Idempotency and Duplicate Protection Bypass",
                                "The same original transaction reference is submitted repeatedly.",
                                List.of(
                                        step("5000", 0, "TXN-DUP-001"),
                                        step("5000", 0, "TXN-DUP-001"),
                                        step("5000", 1, "TXN-DUP-001")
                                )
                        )
                )
        );
    }

    private static GeneratedAttackScenariosResponse.RefundStep step(
            String amount,
            long hoursAfterStart,
            String transactionReference) {
        return new GeneratedAttackScenariosResponse.RefundStep(
                new BigDecimal(amount),
                hoursAfterStart,
                transactionReference
        );
    }

    private static SimulationRunResponse persistedRun() {
        return new SimulationRunResponse(
                UUID.randomUUID(),
                "PS-001",
                1,
                Instant.parse("2026-01-01T00:00:00Z"),
                3,
                10,
                5,
                0,
                5,
                new BigDecimal("27000"),
                3,
                0,
                0,
                List.of(
                        new SimulationRunResponse.AttackMetric("THRESHOLD_SPLITTING", 1, 1),
                        new SimulationRunResponse.AttackMetric("CUMULATIVE_EXPOSURE", 1, 1),
                        new SimulationRunResponse.AttackMetric("DUPLICATE_REQUEST", 1, 1)
                )
        );
    }
}
