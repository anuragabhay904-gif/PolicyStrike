package com.policystrike.simulation;

import com.policystrike.api.dto.AttackScenarioExecutionResponse;
import com.policystrike.api.dto.GeneratedAttackScenariosResponse;
import com.policystrike.api.dto.SimulationRunResponse;
import com.policystrike.domain.DecisionStatus;
import com.policystrike.domain.RefundDecision;
import com.policystrike.domain.RefundPolicy;
import com.policystrike.domain.RefundRequest;
import com.policystrike.engine.RefundPolicyEvaluator;
import com.policystrike.persistence.entity.PolicyVersionEntity;
import com.policystrike.persistence.service.SimulationHistoryService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GeneratedScenarioExecutionService {

    private static final Instant SIMULATION_START =
            Instant.parse("2026-01-01T00:00:00Z");

    private static final long MAX_HOURS_AFTER_START = 24L * 365L * 10L;
    private static final int MAX_SCENARIOS = 5;
    private static final int MAX_STEPS_PER_SCENARIO = 10;
    private static final BigDecimal MAX_PERSISTED_AMOUNT =
            new BigDecimal("99999999999999999.99");

    private final SimulationHistoryService historyService;

    public GeneratedScenarioExecutionService(
            SimulationHistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * Executes previously generated scenario suggestions against the
     * deterministic Java evaluator. Each scenario receives an isolated
     * synthetic customer and evaluator state so scenarios cannot affect
     * one another.
     */
    public AttackScenarioExecutionResponse execute(
            PolicyVersionEntity policyVersionEntity,
            GeneratedAttackScenariosResponse generated) {

        if (policyVersionEntity == null) {
            throw badRequest("A persisted policy version is required");
        }

        RefundPolicy policy = policyVersionEntity.toDomain();
        validateGeneratedResponse(policy, generated);

        List<EvaluationRecord> allEvaluations = new ArrayList<>();
        List<AttackScenarioExecutionResponse.ScenarioResult> scenarioResults =
                new ArrayList<>();

        Map<AttackType, Integer> injectedAttacks =
                new EnumMap<>(AttackType.class);
        Map<AttackType, Integer> detectedAttacks =
                new EnumMap<>(AttackType.class);

        Set<String> restrictedCustomers = new HashSet<>();

        int totalRequests = 0;
        int autoApprovedRequests = 0;
        int manualReviewRequests = 0;
        int blockedRequests = 0;
        int mitigatedScenarios = 0;
        BigDecimal approvedExposure = BigDecimal.ZERO;

        for (int scenarioIndex = 0;
             scenarioIndex < generated.scenarios().size();
             scenarioIndex++) {

            GeneratedAttackScenariosResponse.ScenarioSuggestion scenario =
                    generated.scenarios().get(scenarioIndex);

            List<GeneratedAttackScenariosResponse.RefundStep> orderedSteps =
                    scenario.steps().stream()
                            .sorted(Comparator.comparingLong(
                                    GeneratedAttackScenariosResponse.RefundStep::hoursAfterStart
                            ))
                            .toList();

            String customerId = "AI-" + UUID.randomUUID();

            // Evaluator state is intentionally isolated per attack scenario.
            RefundPolicyEvaluator evaluator = new RefundPolicyEvaluator();

            List<AttackScenarioExecutionResponse.StepResult> stepResults =
                    new ArrayList<>();

            int scenarioApproved = 0;
            int scenarioManualReview = 0;
            int scenarioBlocked = 0;
            BigDecimal scenarioApprovedExposure = BigDecimal.ZERO;
            boolean scenarioMitigated = false;

            for (GeneratedAttackScenariosResponse.RefundStep step : orderedSteps) {

                Instant requestedAt = SIMULATION_START.plus(
                        Duration.ofHours(step.hoursAfterStart())
                );

                String requestId = "AI-" + UUID.randomUUID();

                RefundRequest refundRequest = new RefundRequest(
                        requestId,
                        customerId,
                        step.transactionReference(),
                        step.amount(),
                        requestedAt
                );

                RefundDecision decision =
                        evaluator.evaluate(policy, refundRequest);

                allEvaluations.add(new EvaluationRecord(
                        refundRequest,
                        decision,
                        scenario.attackType()
                ));

                stepResults.add(new AttackScenarioExecutionResponse.StepResult(
                        requestId,
                        customerId,
                        step.transactionReference(),
                        step.amount(),
                        requestedAt,
                        decision.getStatus(),
                        decision.getReason()
                ));

                totalRequests++;

                switch (decision.getStatus()) {
                    case AUTO_APPROVE -> {
                        autoApprovedRequests++;
                        scenarioApproved++;
                        approvedExposure = approvedExposure.add(step.amount());
                        scenarioApprovedExposure =
                                scenarioApprovedExposure.add(step.amount());
                    }
                    case MANUAL_REVIEW -> {
                        manualReviewRequests++;
                        scenarioManualReview++;
                        scenarioMitigated = true;
                    }
                    case BLOCKED -> {
                        blockedRequests++;
                        scenarioBlocked++;
                        scenarioMitigated = true;
                        restrictedCustomers.add(customerId);
                    }
                }
            }

            injectedAttacks.merge(
                    scenario.attackType(), 1, Integer::sum
            );

            if (scenarioMitigated) {
                mitigatedScenarios++;
                detectedAttacks.merge(
                        scenario.attackType(), 1, Integer::sum
                );
            }

            scenarioResults.add(new AttackScenarioExecutionResponse.ScenarioResult(
                    scenario.attackType(),
                    scenario.title(),
                    scenario.description(),
                    orderedSteps.size(),
                    scenarioApproved,
                    scenarioManualReview,
                    scenarioBlocked,
                    scenarioApprovedExposure,
                    scenarioMitigated,
                    stepResults
            ));
        }

        int unmitigatedScenarios =
                generated.scenarios().size() - mitigatedScenarios;

        SimulationReport report = new SimulationReport(
                generated.scenarios().size(),
                totalRequests,
                autoApprovedRequests,
                manualReviewRequests,
                blockedRequests,
                approvedExposure,
                restrictedCustomers.size(),
                injectedAttacks,
                detectedAttacks,
                0,
                unmitigatedScenarios
        );

        SimulationExecution execution = new SimulationExecution(
                report,
                allEvaluations
        );

        SimulationRunResponse persistedRun =
                historyService.saveRun(policyVersionEntity, execution);

        return new AttackScenarioExecutionResponse(
                policy.getPolicyId(),
                policy.getVersion(),
                "EXECUTED",
                "DETERMINISTIC_JAVA_POLICY_ENGINE",
                SIMULATION_START,
                generated.scenarios().size(),
                totalRequests,
                autoApprovedRequests,
                manualReviewRequests,
                blockedRequests,
                approvedExposure,
                mitigatedScenarios,
                unmitigatedScenarios,
                persistedRun,
                scenarioResults
        );
    }

    private void validateGeneratedResponse(
            RefundPolicy policy,
            GeneratedAttackScenariosResponse generated) {

        if (generated == null) {
            throw badRequest("Generated attack scenarios are required");
        }

        if (!policy.getPolicyId().equals(generated.policyId())
                || policy.getVersion() != generated.policyVersion()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Generated scenarios do not match the current persisted policy version. Generate them again."
            );
        }

        if (!"SUGGESTED_NOT_EXECUTED".equals(generated.status())) {
            throw badRequest(
                    "Only scenarios with status SUGGESTED_NOT_EXECUTED can be executed"
            );
        }

        if (generated.scenarios() == null
                || generated.scenarios().isEmpty()
                || generated.scenarios().size() > MAX_SCENARIOS) {
            throw badRequest(
                    "Scenario count must be between 1 and " + MAX_SCENARIOS
            );
        }

        for (GeneratedAttackScenariosResponse.ScenarioSuggestion scenario
                : generated.scenarios()) {
            validateScenario(policy, scenario);
        }
    }

    private void validateScenario(
            RefundPolicy policy,
            GeneratedAttackScenariosResponse.ScenarioSuggestion scenario) {

        if (scenario == null
                || scenario.attackType() == null
                || scenario.attackType() == AttackType.NONE
                || isBlank(scenario.title())
                || scenario.title().length() > 200
                || isBlank(scenario.description())
                || scenario.description().length() > 1500
                || scenario.steps() == null
                || scenario.steps().size() < 2
                || scenario.steps().size() > MAX_STEPS_PER_SCENARIO) {
            throw unprocessable("A generated scenario contains invalid metadata or step count");
        }

        for (GeneratedAttackScenariosResponse.RefundStep step : scenario.steps()) {
            if (step == null
                    || step.amount() == null
                    || step.amount().compareTo(BigDecimal.ZERO) <= 0
                    || step.amount().compareTo(MAX_PERSISTED_AMOUNT) > 0
                    || step.amount().stripTrailingZeros().scale() > 2
                    || step.hoursAfterStart() < 0
                    || step.hoursAfterStart() > MAX_HOURS_AFTER_START
                    || isBlank(step.transactionReference())
                    || step.transactionReference().length() > 150) {
                throw unprocessable("A generated refund step contains invalid values");
            }
        }

        List<GeneratedAttackScenariosResponse.RefundStep> orderedSteps =
                scenario.steps().stream()
                        .sorted(Comparator.comparingLong(
                                GeneratedAttackScenariosResponse.RefundStep::hoursAfterStart
                        ))
                        .toList();

        switch (scenario.attackType()) {
            case THRESHOLD_SPLITTING -> validateThresholdSplitting(policy, orderedSteps);
            case CUMULATIVE_EXPOSURE -> validateCumulativeExposure(policy, orderedSteps);
            case DUPLICATE_REQUEST -> validateDuplicateRequest(orderedSteps);
            case NONE -> throw unprocessable("NONE is not an executable attack category");
        }
    }

    private void validateThresholdSplitting(
            RefundPolicy policy,
            List<GeneratedAttackScenariosResponse.RefundStep> steps) {

        if (steps.stream().anyMatch(step ->
                step.amount().compareTo(policy.getAutomaticRefundLimit()) > 0)) {
            throw unprocessable(
                    "Threshold-splitting steps must each be within the individual automatic refund limit"
            );
        }

        long distinctReferences = steps.stream()
                .map(GeneratedAttackScenariosResponse.RefundStep::transactionReference)
                .distinct()
                .count();

        if (distinctReferences != steps.size()) {
            throw unprocessable(
                    "Threshold-splitting scenarios must use distinct transaction references"
            );
        }

        if (!rollingWindowExceeds(
                steps,
                policy.getThresholdSplittingWindow(),
                policy.getAutomaticRefundLimit())) {
            throw unprocessable(
                    "Threshold-splitting steps do not exceed the automatic limit within the configured monitoring window"
            );
        }
    }

    private void validateCumulativeExposure(
            RefundPolicy policy,
            List<GeneratedAttackScenariosResponse.RefundStep> steps) {

        if (steps.stream().anyMatch(step ->
                step.amount().compareTo(policy.getAutomaticRefundLimit()) > 0)) {
            throw unprocessable(
                    "Cumulative-exposure steps must each be within the individual automatic refund limit"
            );
        }

        if (!rollingWindowExceeds(
                steps,
                policy.getCumulativeExposureWindow(),
                policy.getCustomerCumulativeLimit())) {
            throw unprocessable(
                    "Cumulative-exposure steps do not exceed the configured customer limit within its monitoring window"
            );
        }

        if (rollingWindowExceeds(
                steps,
                policy.getThresholdSplittingWindow(),
                policy.getAutomaticRefundLimit())) {
            throw unprocessable(
                    "Cumulative-exposure scenario also triggers threshold splitting; adjust its timings to isolate the cumulative attack"
            );
        }

        long distinctReferences = steps.stream()
                .map(GeneratedAttackScenariosResponse.RefundStep::transactionReference)
                .distinct()
                .count();

        if (distinctReferences != steps.size()) {
            throw unprocessable(
                    "Cumulative-exposure scenarios must use distinct transaction references"
            );
        }
    }

    private void validateDuplicateRequest(
            List<GeneratedAttackScenariosResponse.RefundStep> steps) {

        Set<String> references = new HashSet<>();
        boolean duplicateFound = false;

        for (GeneratedAttackScenariosResponse.RefundStep step : steps) {
            if (!references.add(step.transactionReference())) {
                duplicateFound = true;
                break;
            }
        }

        if (!duplicateFound) {
            throw unprocessable(
                    "Duplicate-request scenarios must repeat at least one transaction reference"
            );
        }
    }

    private boolean rollingWindowExceeds(
            List<GeneratedAttackScenariosResponse.RefundStep> sortedSteps,
            Duration window,
            BigDecimal limit) {

        for (GeneratedAttackScenariosResponse.RefundStep current : sortedSteps) {
            BigDecimal rollingTotal = BigDecimal.ZERO;

            for (GeneratedAttackScenariosResponse.RefundStep candidate : sortedSteps) {
                long hoursBeforeCurrent =
                        current.hoursAfterStart() - candidate.hoursAfterStart();

                if (hoursBeforeCurrent >= 0
                        && Duration.ofHours(hoursBeforeCurrent).compareTo(window) <= 0) {
                    rollingTotal = rollingTotal.add(candidate.amount());
                }
            }

            if (rollingTotal.compareTo(limit) > 0) {
                return true;
            }
        }

        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException unprocessable(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
