package com.policystrike.api.dto;

import java.math.BigDecimal;

public record CreatePolicyVersionRequest(
        BigDecimal automaticRefundLimit,
        BigDecimal customerCumulativeLimit,
        Long thresholdSplittingWindowHours,
        Long cumulativeExposureWindowDays,
        Boolean duplicateProtectionEnabled
) {
}
