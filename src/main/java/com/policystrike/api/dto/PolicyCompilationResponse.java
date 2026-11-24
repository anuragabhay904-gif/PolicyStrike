
package com.policystrike.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record PolicyCompilationResponse(
        String policyId,
        BigDecimal automaticRefundLimit,
        BigDecimal customerCumulativeLimit,
        Long thresholdSplittingWindowHours,
        Long cumulativeExposureWindowDays,
        Boolean duplicateProtectionEnabled,
        List<String> assumptions,
        List<String> ambiguities,
        String rationale,
        boolean validProposal,
        boolean requiresHumanApproval
) {
}
