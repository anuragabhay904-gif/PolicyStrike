package com.policystrike.simulation;

import com.policystrike.domain.DecisionStatus;
import com.policystrike.domain.RefundDecision;
import com.policystrike.domain.RefundPolicy;
import com.policystrike.domain.RefundRequest;
import com.policystrike.engine.RefundPolicyEvaluator;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class PolicyVersionReplayService {

    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private final PolicyVersionRepository repository;

    public PolicyVersionReplayService(PolicyVersionRepository repository) {
        this.repository = repository;
    }

    /** Compares the two newest non-rejected persisted versions for a policy. */
    @Transactional(readOnly = true)
    public List<AttackReplayResult> replayLatestPair(String policyId) {
        List<PolicyVersionEntity> versions = repository
                .findAllByPolicyIdOrderByVersionDesc(policyId)
                .stream()
                .filter(version -> version.getStatus() != PolicyVersionStatus.REJECTED)
                .toList();

        if (versions.size() < 2) {
            return List.of();
        }

        int candidateVersion = versions.get(0).getVersion();
        int baselineVersion = versions.get(1).getVersion();
        return replay(policyId, baselineVersion, candidateVersion);
    }

    /**
     * Replays the same deterministic attack suite against two stored versions.
     * The candidate may be PENDING_APPROVAL so reviewers can test it before activation.
     */
    @Transactional(readOnly = true)
    public List<AttackReplayResult> replay(
            String policyId,
            int baselineVersion,
            int candidateVersion) {

        if (policyId == null || policyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy ID is required");
        }
        if (baselineVersion <= 0 || candidateVersion <= baselineVersion) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Candidate version must be greater than the baseline version"
            );
        }

        PolicyVersionEntity baselineEntity = findVersion(policyId, baselineVersion);
        PolicyVersionEntity candidateEntity = findVersion(policyId, candidateVersion);

        if (baselineEntity.getStatus() == PolicyVersionStatus.REJECTED
                || candidateEntity.getStatus() == PolicyVersionStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Rejected policy versions cannot be used in a replay"
            );
        }

        RefundPolicy baselinePolicy = baselineEntity.toDomain();
        RefundPolicy candidatePolicy = candidateEntity.toDomain();

        return attackSuite().stream()
                .map(scenario -> compare(
                        scenario, baselinePolicy, candidatePolicy))
                .toList();
    }

    /** Backward-compatible default used by the replay API and explanation feature. */
    @Transactional(readOnly = true)
    public List<AttackReplayResult> replayAll() {
        return replayLatestPair("PS-001");
    }

    private PolicyVersionEntity findVersion(String policyId, int version) {
        return repository.findByPolicyIdAndVersion(policyId, version)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Policy version not found: " + policyId + " v" + version
                ));
    }

    private AttackReplayResult compare(
            AttackScenario scenario,
            RefundPolicy baselinePolicy,
            RefundPolicy candidatePolicy) {

        ScenarioEvaluation baseline = evaluate(baselinePolicy, scenario);
        ScenarioEvaluation candidate = evaluate(candidatePolicy, scenario);

        boolean baselineMitigated = baseline.blocked() + baseline.manualReview() > 0;
        boolean candidateMitigated = candidate.blocked() + candidate.manualReview() > 0;
        // A candidate demonstrates improvement when it sends fewer requests to auto-approval.
        // The baseline may already block a later step in the same attack sequence.
        boolean remediationVerified = candidateMitigated
                && candidate.autoApproved() < baseline.autoApproved();

        return new AttackReplayResult(
                scenario.attackType(),
                baselinePolicy.getVersion(),
                baseline.outcome(),
                baseline.autoApproved(),
                baseline.manualReview(),
                baseline.blocked(),
                candidatePolicy.getVersion(),
                candidate.outcome(),
                candidate.autoApproved(),
                candidate.manualReview(),
                candidate.blocked(),
                baselineMitigated,
                candidateMitigated,
                remediationVerified
        );
    }

    private ScenarioEvaluation evaluate(RefundPolicy policy, AttackScenario scenario) {
        RefundPolicyEvaluator evaluator = new RefundPolicyEvaluator();
        int approved = 0;
        int manualReview = 0;
        int blocked = 0;

        for (RefundRequest request : scenario.requests()) {
            RefundDecision decision = evaluator.evaluate(policy, request);
            switch (decision.getStatus()) {
                case AUTO_APPROVE -> approved++;
                case MANUAL_REVIEW -> manualReview++;
                case BLOCKED -> blocked++;
            }
        }

        DecisionStatus outcome = blocked > 0
                ? DecisionStatus.BLOCKED
                : manualReview > 0
                    ? DecisionStatus.MANUAL_REVIEW
                    : DecisionStatus.AUTO_APPROVE;

        return new ScenarioEvaluation(outcome, approved, manualReview, blocked);
    }

    private List<AttackScenario> attackSuite() {
        return List.of(
                thresholdSplittingScenario(),
                cumulativeExposureScenario(),
                duplicateRequestScenario()
        );
    }

    // Six individually-small refunds exceed the original 24-hour window limit.
    private AttackScenario thresholdSplittingScenario() {
        String customerId = "REPLAY-CUSTOMER-THRESHOLD";
        List<RefundRequest> requests = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            requests.add(request(
                    "REPLAY-SPLIT-" + (index + 1),
                    customerId,
                    "REPLAY-TXN-SPLIT-" + (index + 1),
                    "1500",
                    BASE_TIME.plus(Duration.ofHours(index * 2L))
            ));
        }
        return new AttackScenario(AttackType.THRESHOLD_SPLITTING, requests);
    }

    // Four weekly refunds remain within the 30-day exposure window.
    private AttackScenario cumulativeExposureScenario() {
        String customerId = "REPLAY-CUSTOMER-CUMULATIVE";
        List<RefundRequest> requests = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            requests.add(request(
                    "REPLAY-CUM-" + (index + 1),
                    customerId,
                    "REPLAY-TXN-CUM-" + (index + 1),
                    "1500",
                    BASE_TIME.plus(Duration.ofDays(index * 7L))
            ));
        }
        return new AttackScenario(AttackType.CUMULATIVE_EXPOSURE, requests);
    }

    private AttackScenario duplicateRequestScenario() {
        String customerId = "REPLAY-CUSTOMER-DUPLICATE";
        String transactionId = "REPLAY-PAYMENT-DUPLICATE";
        return new AttackScenario(
                AttackType.DUPLICATE_REQUEST,
                List.of(
                        request("REPLAY-DUP-1", customerId, transactionId,
                                "1000", BASE_TIME),
                        request("REPLAY-DUP-2", customerId, transactionId,
                                "1000", BASE_TIME.plus(Duration.ofHours(1))),
                        request("REPLAY-DUP-3", customerId, transactionId,
                                "1000", BASE_TIME.plus(Duration.ofHours(2)))
                )
        );
    }

    private RefundRequest request(
            String requestId,
            String customerId,
            String transactionId,
            String amount,
            Instant requestedAt) {
        return new RefundRequest(
                requestId,
                customerId,
                transactionId,
                new BigDecimal(amount),
                requestedAt
        );
    }

    private record ScenarioEvaluation(
            DecisionStatus outcome,
            int autoApproved,
            int manualReview,
            int blocked) {
    }
}
