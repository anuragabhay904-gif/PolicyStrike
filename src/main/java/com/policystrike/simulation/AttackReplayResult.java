package com.policystrike.simulation;

import com.policystrike.domain.DecisionStatus;

public record AttackReplayResult(
        AttackType attackType,
        int baselineVersion,
        DecisionStatus baselineOutcome,
        int baselineAutoApprovedRequests,
        int baselineManualReviewRequests,
        int baselineBlockedRequests,
        int candidateVersion,
        DecisionStatus candidateOutcome,
        int candidateAutoApprovedRequests,
        int candidateManualReviewRequests,
        int candidateBlockedRequests,
        boolean baselineMitigated,
        boolean candidateMitigated,
        boolean remediationVerified
) {
}
