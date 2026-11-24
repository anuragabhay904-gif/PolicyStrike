
package com.policystrike.ai;

import tools.jackson.databind.json.JsonMapper;

import com.policystrike.api.dto.CompilePolicyRequest;
import com.policystrike.api.dto.PolicyCompilationResponse;

import com.policystrike.domain.RefundPolicy;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class PolicyCompilationService {

    private final GeminiGateway gemini;
    private final JsonMapper JsonMapper;

    public PolicyCompilationService(
            GeminiGateway gemini,
            JsonMapper objectMapper) {

        this.gemini = gemini;
        this.JsonMapper = objectMapper;
    }

    private static final Map<String, Object> POLICY_SCHEMA =
            Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "automaticRefundLimit",
                            Map.of("type", "number"),

                            "customerCumulativeLimit",
                            Map.of("type", "number"),

                            "thresholdSplittingWindowHours",
                            Map.of("type", "integer"),

                            "cumulativeExposureWindowDays",
                            Map.of("type", "integer"),

                            "duplicateProtectionEnabled",
                            Map.of("type", "boolean"),

                            "assumptions",
                            Map.of(
                                    "type", "array",
                                    "items", Map.of("type", "string")
                            ),

                            "ambiguities",
                            Map.of(
                                    "type", "array",
                                    "items", Map.of("type", "string")
                            ),

                            "rationale",
                            Map.of("type", "string")
                    ),
                    "required", List.of(
                            "automaticRefundLimit",
                            "customerCumulativeLimit",
                            "thresholdSplittingWindowHours",
                            "cumulativeExposureWindowDays",
                            "duplicateProtectionEnabled",
                            "assumptions",
                            "ambiguities",
                            "rationale"
                    )
            );

    public PolicyCompilationResponse compile(
            CompilePolicyRequest request) {

        if (request == null ||
                request.policyId() == null ||
                request.policyId().isBlank() ||
                request.policyText() == null ||
                request.policyText().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Policy ID and policy text are required"
            );
        }

        if (request.policyText().length() > 10000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Policy text cannot exceed 10,000 characters"
            );
        }

        String prompt = """
                You are a financial policy extraction assistant.

                Convert the merchant's natural-language refund policy
                into the requested structured fields.

                STRICT RULES:
                1. Extract only rules supported by the supplied policy.
                2. Do not invent limits or time windows.
                3. If a numeric rule is not specified, return 0.
                4. Record every missing or ambiguous rule in ambiguities.
                5. Make monetary values numeric, in the policy currency.
                6. Return only the JSON structure requested.
                7. Do not decide whether any refund should be approved.

                Policy ID: %s

                Merchant policy:
                %s
                """.formatted(
                request.policyId(),
                request.policyText()
        );

        String json = gemini.generateJson(prompt, POLICY_SCHEMA);

        try {

            PolicyDraft draft =
                    JsonMapper.readValue(json, PolicyDraft.class);

            List<String> assumptions =
                    draft.assumptions() == null
                            ? List.of()
                            : draft.assumptions();

            List<String> ambiguities =
                    new java.util.ArrayList<>(
                            draft.ambiguities() == null
                                    ? List.of()
                                    : draft.ambiguities()
                    );

            boolean valid = false;

            try {

                new RefundPolicy(
                        request.policyId(),
                        1,
                        draft.automaticRefundLimit(),
                        draft.customerCumulativeLimit(),
                        Duration.ofHours(
                                requiredPositive(
                                        draft.thresholdSplittingWindowHours())
                        ),
                        Duration.ofDays(
                                requiredPositive(
                                        draft.cumulativeExposureWindowDays())
                        ),
                        requireBoolean(
                                draft.duplicateProtectionEnabled())
                );

                valid = ambiguities.isEmpty();

            } catch (IllegalArgumentException exception) {

                ambiguities.add(exception.getMessage());

            }

            return new PolicyCompilationResponse(
                    request.policyId(),
                    draft.automaticRefundLimit(),
                    draft.customerCumulativeLimit(),
                    draft.thresholdSplittingWindowHours(),
                    draft.cumulativeExposureWindowDays(),
                    draft.duplicateProtectionEnabled(),
                    assumptions,
                    ambiguities,
                    draft.rationale(),
                    valid,
                    true
            );

        } catch (Exception exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not parse or validate Gemini policy output",
                    exception
            );
        }
    }

    private long requiredPositive(Long value) {

        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                    "A valid positive monitoring window is required"
            );
        }

        return value;
    }

    private boolean requireBoolean(Boolean value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Duplicate protection configuration is required"
            );
        }

        return value;
    }

    private record PolicyDraft(
            BigDecimal automaticRefundLimit,
            BigDecimal customerCumulativeLimit,
            Long thresholdSplittingWindowHours,
            Long cumulativeExposureWindowDays,
            Boolean duplicateProtectionEnabled,
            List<String> assumptions,
            List<String> ambiguities,
            String rationale
    ) {
    }
}
