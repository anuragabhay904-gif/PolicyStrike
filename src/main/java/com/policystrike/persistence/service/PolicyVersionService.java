package com.policystrike.persistence.service;

import com.policystrike.api.dto.CreatePolicyVersionRequest;
import com.policystrike.api.dto.PolicyVersionResponse;
import com.policystrike.domain.RefundPolicy;
import com.policystrike.persistence.entity.PolicyVersionEntity;
import com.policystrike.persistence.entity.PolicyVersionStatus;
import com.policystrike.persistence.repository.PolicyVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class PolicyVersionService {

    private final PolicyVersionRepository repository;

    public PolicyVersionService(PolicyVersionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void initializeDefaultPolicy() {
        if (repository.existsByPolicyId("PS-001")) {
            return;
        }

        RefundPolicy defaultPolicy = new RefundPolicy(
                "PS-001", 1,
                new BigDecimal("7500"),
                new BigDecimal("20000"),
                Duration.ofHours(24),
                Duration.ofDays(30),
                true
        );
        repository.save(PolicyVersionEntity.from(defaultPolicy));
    }

    /** Returns the currently approved policy used for live simulations. */
    @Transactional(readOnly = true)
    public PolicyVersionEntity getActiveVersion(String policyId) {
        return repository.findFirstByPolicyIdAndStatusOrderByVersionDesc(
                        policyId, PolicyVersionStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No active policy version found for: " + policyId
                ));
    }

    /** Kept as a safe alias: runtime evaluation must never select a pending version. */
    @Transactional(readOnly = true)
    public PolicyVersionEntity getLatestVersion(String policyId) {
        return getActiveVersion(policyId);
    }

    @Transactional(readOnly = true)
    public PolicyVersionEntity getVersion(String policyId, int version) {
        return repository.findByPolicyIdAndVersion(policyId, version)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Policy version not found: " + policyId + " v" + version
                ));
    }

    @Transactional(readOnly = true)
    public List<PolicyVersionResponse> getVersions(String policyId) {
        return repository.findAllByPolicyIdOrderByVersionDesc(policyId)
                .stream()
                .map(PolicyVersionResponse::from)
                .toList();
    }

    /** Creates a proposal only. It does not become active until approved. */
    @Transactional
    public PolicyVersionResponse createNextVersion(
            String policyId,
            CreatePolicyVersionRequest request) {

        if (policyId == null || policyId.isBlank()) {
            throw badRequest("Policy ID is required");
        }
        if (!repository.existsByPolicyId(policyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Policy not found: " + policyId);
        }
        if (request == null) {
            throw badRequest("Policy configuration is required");
        }
        if (request.automaticRefundLimit() == null
                || request.customerCumulativeLimit() == null
                || request.thresholdSplittingWindowHours() == null
                || request.cumulativeExposureWindowDays() == null
                || request.duplicateProtectionEnabled() == null) {
            throw badRequest("All policy configuration fields are required");
        }
        if (repository.existsByPolicyIdAndStatus(
                policyId, PolicyVersionStatus.PENDING_APPROVAL)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A policy version is already awaiting approval");
        }

        int nextVersion = repository.findFirstByPolicyIdOrderByVersionDesc(policyId)
                .map(existing -> existing.getVersion() + 1)
                .orElse(1);

        final RefundPolicy policy;
        try {
            policy = new RefundPolicy(
                    policyId,
                    nextVersion,
                    request.automaticRefundLimit(),
                    request.customerCumulativeLimit(),
                    Duration.ofHours(request.thresholdSplittingWindowHours()),
                    Duration.ofDays(request.cumulativeExposureWindowDays()),
                    request.duplicateProtectionEnabled()
            );
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw badRequest("Invalid policy configuration: " + exception.getMessage());
        }

        PolicyVersionEntity pending = repository.save(PolicyVersionEntity.pending(policy));
        return PolicyVersionResponse.from(pending);
    }

    @Transactional
    public PolicyVersionResponse approveVersion(
            String policyId,
            int version,
            String approvedBy) {

        if (approvedBy == null || approvedBy.isBlank() || approvedBy.trim().length() > 120) {
            throw badRequest("approvedBy is required and must not exceed 120 characters");
        }

        PolicyVersionEntity target = getVersion(policyId, version);
        if (target.getStatus() != PolicyVersionStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING_APPROVAL versions can be approved");
        }

        List<PolicyVersionEntity> activeVersions =
                repository.findAllByPolicyIdAndStatusOrderByVersionDesc(
                        policyId, PolicyVersionStatus.ACTIVE);
        activeVersions.forEach(PolicyVersionEntity::supersede);
        repository.saveAll(activeVersions);

        target.approve(approvedBy, Instant.now());
        return PolicyVersionResponse.from(repository.save(target));
    }

    @Transactional
    public PolicyVersionResponse rejectVersion(
            String policyId,
            int version,
            String rejectedBy,
            String reason) {

        if (rejectedBy == null || rejectedBy.isBlank() || rejectedBy.trim().length() > 120) {
            throw badRequest("rejectedBy is required and must not exceed 120 characters");
        }
        if (reason == null || reason.isBlank() || reason.trim().length() > 1000) {
            throw badRequest("A rejection reason is required and must not exceed 1000 characters");
        }

        PolicyVersionEntity target = getVersion(policyId, version);
        try {
            target.reject(rejectedBy, reason, Instant.now());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return PolicyVersionResponse.from(repository.save(target));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
