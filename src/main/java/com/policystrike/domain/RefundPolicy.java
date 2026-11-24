package com.policystrike.domain;

import java.math.BigDecimal;
import java.time.Duration;

public final class RefundPolicy {

    private final String policyId;
    private final int version;
    private final BigDecimal automaticRefundLimit;
    private final BigDecimal customerCumulativeLimit;
    private final Duration thresholdSplittingWindow;
    private final Duration cumulativeExposureWindow;
    private final boolean duplicateProtectionEnabled;

    // Retains compatibility with our existing simulation.
    public RefundPolicy(
            String policyId,
            int version,
            BigDecimal automaticRefundLimit,
            BigDecimal customerCumulativeLimit,
            Duration thresholdSplittingWindow,
            Duration cumulativeExposureWindow) {

        this(
                policyId,
                version,
                automaticRefundLimit,
                customerCumulativeLimit,
                thresholdSplittingWindow,
                cumulativeExposureWindow,
                true
        );
    }

    public RefundPolicy(
            String policyId,
            int version,
            BigDecimal automaticRefundLimit,
            BigDecimal customerCumulativeLimit,
            Duration thresholdSplittingWindow,
            Duration cumulativeExposureWindow,
            boolean duplicateProtectionEnabled) {

        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("Policy ID is required");
        }

        if (version <= 0) {
            throw new IllegalArgumentException("Policy version must be positive");
        }

        if (automaticRefundLimit == null ||
                automaticRefundLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Automatic refund limit must be positive");
        }

        if (customerCumulativeLimit == null ||
                customerCumulativeLimit.compareTo(automaticRefundLimit) <= 0) {
            throw new IllegalArgumentException(
                    "Cumulative limit must exceed automatic refund limit");
        }

        if (thresholdSplittingWindow == null ||
                thresholdSplittingWindow.isZero() ||
                thresholdSplittingWindow.isNegative()) {
            throw new IllegalArgumentException("Splitting window must be positive");
        }

        if (cumulativeExposureWindow == null ||
                cumulativeExposureWindow.isZero() ||
                cumulativeExposureWindow.isNegative()) {
            throw new IllegalArgumentException("Exposure window must be positive");
        }

        this.policyId = policyId;
        this.version = version;
        this.automaticRefundLimit = automaticRefundLimit;
        this.customerCumulativeLimit = customerCumulativeLimit;
        this.thresholdSplittingWindow = thresholdSplittingWindow;
        this.cumulativeExposureWindow = cumulativeExposureWindow;
        this.duplicateProtectionEnabled = duplicateProtectionEnabled;
    }

    public String getPolicyId() {
        return policyId;
    }

    public int getVersion() {
        return version;
    }

    public BigDecimal getAutomaticRefundLimit() {
        return automaticRefundLimit;
    }

    public BigDecimal getCustomerCumulativeLimit() {
        return customerCumulativeLimit;
    }

    public Duration getThresholdSplittingWindow() {
        return thresholdSplittingWindow;
    }

    public Duration getCumulativeExposureWindow() {
        return cumulativeExposureWindow;
    }

    public boolean isDuplicateProtectionEnabled() {
        return duplicateProtectionEnabled;
    }
}
