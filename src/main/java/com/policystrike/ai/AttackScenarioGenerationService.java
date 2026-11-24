
package com.policystrike.ai;

import tools.jackson.databind.json.JsonMapper;

import com.policystrike.api.dto.GenerateAttackScenariosRequest;
import com.policystrike.api.dto.GeneratedAttackScenariosResponse;

import com.policystrike.domain.RefundPolicy;
import com.policystrike.simulation.AttackType;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AttackScenarioGenerationService {

    private final GeminiGateway gemini;
    private final JsonMapper objectMapper;

    public AttackScenarioGenerationService(
            GeminiGateway gemini,
            JsonMapper objectMapper) {

        this.gemini = gemini;
        this.objectMapper = objectMapper;
    }

    private static final Map<String, Object> SCENARIO_SCHEMA =
            Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "scenarios",
                            Map.of(
                                    "type", "array",
                                    "items",
                                    Map.of(
                                            "type", "object",
                                            "properties", Map.of(
                                                    "attackType",
                                                    Map.of(
                                                            "type", "string",
                                                            "enum", List.of(
                                                                    "THRESHOLD_SPLITTING",
                                                                    "CUMULATIVE_EXPOSURE",
                                                                    "DUPLICATE_REQUEST"
                                                            )
                                                    ),
                                                    "title",
                                                    Map.of("type", "string"),
                                                    "description",
                                                    Map.of("type", "string"),
                                                    "steps",
                                                    Map.of(
                                                            "type", "array",
                                                            "items",
                                                            Map.of(
                                                                    "type", "object",
                                                                    "properties", Map.of(
                                                                            "amount",
                                                                            Map.of("type", "number"),
                                                                            "hoursAfterStart",
                                                                            Map.of("type", "integer"),
                                                                            "transactionReference",
                                                                            Map.of("type", "string")
                                                                    ),
                                                                    "required", List.of(
                                                                            "amount",
                                                                            "hoursAfterStart",
                                                                            "transactionReference"
                                                                    )
                                                            )
                                                    )
                                            ),
                                            "required", List.of(
                                                    "attackType",
                                                    "title",
                                                    "description",
                                                    "steps"
                                            )
                                    )
                            )
                    ),
                    "required", List.of("scenarios")
            );

    public GeneratedAttackScenariosResponse generate(
            RefundPolicy policy,
            GenerateAttackScenariosRequest request) {

        Objects.requireNonNull(policy, "Policy is required");

        if (request == null ||
                request.context() == null ||
                request.context().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Scenario generation context is required"
            );
        }

        if (request.context().length() > 3000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Scenario context cannot exceed 3,000 characters"
            );
        }

        int count = request.scenarioCount() == null
                ? 3
                : request.scenarioCount();

        if (count < 1 || count > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Scenario count must be between 1 and 5"
            );
        }

        String prompt = """
                You are a financial fraud red-team analyst.

                Generate exactly %d adversarial refund scenarios.

                Available attack categories:
                - THRESHOLD_SPLITTING
                - CUMULATIVE_EXPOSURE
                - DUPLICATE_REQUEST

                Merchant policy:
                Policy ID: %s
                Version: %d
                Automatic refund limit: %s
                Cumulative customer limit: %s
                Threshold splitting window (hours): %d
                Cumulative exposure window (seconds): %d
                Duplicate protection enabled: %s

                Scenario generation context:
                %s

                Requirements:
                1. Generate practical, technically plausible attack sequences.
                2. Each step must have a positive monetary amount.
                3. hoursAfterStart must be zero or positive.
                4. Use repeated transaction references for duplicate-request attacks.
                5. Use different transaction references for the other categories.
                6. Explain the weakness being tested.
                7. Return only the requested structured JSON.
                8. These are suggestions, not financial decisions.
                """.formatted(
                count,
                policy.getPolicyId(),
                policy.getVersion(),
                policy.getAutomaticRefundLimit().toPlainString(),
                policy.getCustomerCumulativeLimit().toPlainString(),
                policy.getThresholdSplittingWindow().toHours(),
                policy.getCumulativeExposureWindow().getSeconds(),
                policy.isDuplicateProtectionEnabled(),
                request.context()
        );

        String json = gemini.generateJson(
                prompt,
                SCENARIO_SCHEMA
        );

        try {

            ScenarioDraft draft =
                    objectMapper.readValue(json, ScenarioDraft.class);

            if (draft.scenarios() == null ||
                    draft.scenarios().size() != count) {

                throw new IllegalArgumentException(
                        "Gemini must return exactly " + count + " scenarios"
                );
            }

            List<GeneratedAttackScenariosResponse.ScenarioSuggestion>
                    suggestions = new ArrayList<>();

            for (ScenarioItemDraft item : draft.scenarios()) {

                AttackType attackType;

                try {
                    attackType = AttackType.valueOf(
                            item.attackType().toUpperCase(Locale.ROOT)
                    );
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException(
                            "Unsupported attack category"
                    );
                }

                if (attackType == AttackType.NONE) {
                    throw new IllegalArgumentException(
                            "NONE is not a valid attack category"
                    );
                }

                if (item.title() == null || item.title().isBlank()
                        || item.description() == null
                        || item.description().isBlank()
                        || item.steps() == null
                        || item.steps().isEmpty()
                        || item.steps().size() > 10) {

                    throw new IllegalArgumentException(
                            "Invalid attack scenario details"
                    );
                }

                List<GeneratedAttackScenariosResponse.RefundStep>
                        steps = new ArrayList<>();

                for (StepDraft step : item.steps()) {

                    if (step.amount() == null ||
                            step.amount().compareTo(BigDecimal.ZERO) <= 0 ||
                            step.hoursAfterStart() == null ||
                            step.hoursAfterStart() < 0 ||
                            step.transactionReference() == null ||
                            step.transactionReference().isBlank()) {

                        throw new IllegalArgumentException(
                                "Invalid refund step in generated scenario"
                        );
                    }

                    steps.add(
                            new GeneratedAttackScenariosResponse.RefundStep(
                                    step.amount(),
                                    step.hoursAfterStart(),
                                    step.transactionReference()
                            )
                    );
                }

                suggestions.add(
                        new GeneratedAttackScenariosResponse.ScenarioSuggestion(
                                attackType,
                                item.title(),
                                item.description(),
                                List.copyOf(steps)
                        )
                );
            }

            return new GeneratedAttackScenariosResponse(
                    policy.getPolicyId(),
                    policy.getVersion(),
                    "SUGGESTED_NOT_EXECUTED",
                    List.copyOf(suggestions)
            );

        } catch (Exception exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not parse or validate Gemini scenario output",
                    exception
            );
        }
    }

    private record ScenarioDraft(
            List<ScenarioItemDraft> scenarios
    ) {
    }

    private record ScenarioItemDraft(
            String attackType,
            String title,
            String description,
            List<StepDraft> steps
    ) {
    }

    private record StepDraft(
            BigDecimal amount,
            Long hoursAfterStart,
            String transactionReference
    ) {
    }
}
