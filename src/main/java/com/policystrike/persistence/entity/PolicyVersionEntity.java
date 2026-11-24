package com.policystrike.persistence.entity;

import com.policystrike.domain.RefundPolicy;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(
    name = "policy_versions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_policy_version",
            columnNames = {"policy_id", "version_number"}
        )
    }
)
public class PolicyVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_id", nullable = false, length = 80)
    private String policyId;

    @Column(name = "version_number", nullable = false)
    private int version;

    @Column(name = "automatic_refund_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal automaticRefundLimit;

    @Column(name = "customer_cumulative_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal customerCumulativeLimit;

    @Column(name = "splitting_window_seconds", nullable = false)
    private long thresholdSplittingWindowSeconds;

    @Column(name = "cumulative_window_seconds", nullable = false)
    private long cumulativeExposureWindowSeconds;

    @Column(name = "duplicate_protection_enabled", nullable = false)
    private boolean duplicateProtectionEnabled;

    // Existing rows are treated as ACTIVE when Hibernate adds this column.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30,
            columnDefinition = "varchar(30) default 'ACTIVE'")
    private PolicyVersionStatus status = PolicyVersionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by", length = 120)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    protected PolicyVersionEntity() {
    }

    private PolicyVersionEntity(RefundPolicy policy, PolicyVersionStatus initialStatus) {
        this.policyId = policy.getPolicyId();
        this.version = policy.getVersion();
        this.automaticRefundLimit = policy.getAutomaticRefundLimit();
        this.customerCumulativeLimit = policy.getCustomerCumulativeLimit();
        this.thresholdSplittingWindowSeconds = policy.getThresholdSplittingWindow().getSeconds();
        this.cumulativeExposureWindowSeconds = policy.getCumulativeExposureWindow().getSeconds();
        this.duplicateProtectionEnabled = policy.isDuplicateProtectionEnabled();
        this.status = initialStatus;
        this.createdAt = Instant.now();
    }

    public static PolicyVersionEntity from(RefundPolicy policy) {
        return new PolicyVersionEntity(policy, PolicyVersionStatus.ACTIVE);
    }

    public static PolicyVersionEntity pending(RefundPolicy policy) {
        return new PolicyVersionEntity(policy, PolicyVersionStatus.PENDING_APPROVAL);
    }

    public RefundPolicy toDomain() {
        return new RefundPolicy(
                policyId,
                version,
                automaticRefundLimit,
                customerCumulativeLimit,
                Duration.ofSeconds(thresholdSplittingWindowSeconds),
                Duration.ofSeconds(cumulativeExposureWindowSeconds),
                duplicateProtectionEnabled
        );
    }

    public void approve(String reviewer, Instant approvedTime) {
        requireStatus(PolicyVersionStatus.PENDING_APPROVAL, "Only pending versions can be approved");
        this.status = PolicyVersionStatus.ACTIVE;
        this.approvedBy = requireReviewer(reviewer);
        this.approvedAt = approvedTime;
        this.rejectedBy = null;
        this.rejectedAt = null;
        this.rejectionReason = null;
    }

    public void supersede() {
        if (this.status == PolicyVersionStatus.ACTIVE) {
            this.status = PolicyVersionStatus.SUPERSEDED;
        }
    }

    public void reject(String reviewer, String reason, Instant rejectedTime) {
        requireStatus(PolicyVersionStatus.PENDING_APPROVAL, "Only pending versions can be rejected");
        this.status = PolicyVersionStatus.REJECTED;
        this.rejectedBy = requireReviewer(reviewer);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        String normalizedReason = reason.trim();
        if (normalizedReason.length() > 1000) {
            throw new IllegalArgumentException("Rejection reason cannot exceed 1000 characters");
        }
        this.rejectionReason = normalizedReason;
        this.rejectedAt = rejectedTime;
    }

    private void requireStatus(PolicyVersionStatus expected, String message) {
        if (this.status != expected) {
            throw new IllegalStateException(message + ". Current status: " + this.status);
        }
    }

    private String requireReviewer(String reviewer) {
        if (reviewer == null || reviewer.isBlank()) {
            throw new IllegalArgumentException("Reviewer identity is required");
        }
        String normalized = reviewer.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Reviewer identity cannot exceed 120 characters");
        }
        return normalized;
    }

    public Long getId() { return id; }
    public String getPolicyId() { return policyId; }
    public int getVersion() { return version; }
    public BigDecimal getAutomaticRefundLimit() { return automaticRefundLimit; }
    public BigDecimal getCustomerCumulativeLimit() { return customerCumulativeLimit; }
    public long getThresholdSplittingWindowSeconds() { return thresholdSplittingWindowSeconds; }
    public long getCumulativeExposureWindowSeconds() { return cumulativeExposureWindowSeconds; }
    public boolean isDuplicateProtectionEnabled() { return duplicateProtectionEnabled; }
    public PolicyVersionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getRejectedBy() { return rejectedBy; }
    public Instant getRejectedAt() { return rejectedAt; }
    public String getRejectionReason() { return rejectionReason; }
}
