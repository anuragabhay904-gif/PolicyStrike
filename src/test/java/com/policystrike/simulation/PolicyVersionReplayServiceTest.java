package com.policystrike.simulation;

import com.policystrike.domain.RefundPolicy;
import com.policystrike.persistence.entity.PolicyVersionEntity;
import com.policystrike.persistence.entity.PolicyVersionStatus;
import com.policystrike.persistence.repository.PolicyVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PolicyVersionReplayServiceTest {

    private PolicyVersionRepository repository;
    private PolicyVersionReplayService service;
    private PolicyVersionEntity baseline;
    private PolicyVersionEntity candidate;

    @BeforeEach
    void setUp() {
        repository = mock(PolicyVersionRepository.class);
        service = new PolicyVersionReplayService(repository);
        baseline = PolicyVersionEntity.from(new RefundPolicy(
                "PS-001", 1,
                new BigDecimal("7500"),
                new BigDecimal("20000"),
                Duration.ofHours(24), Duration.ofDays(30), true));
        candidate = PolicyVersionEntity.pending(new RefundPolicy(
                "PS-001", 2,
                new BigDecimal("2000"),
                new BigDecimal("5000"),
                Duration.ofHours(24), Duration.ofDays(30), true));
        when(repository.findByPolicyIdAndVersion("PS-001", 1))
                .thenReturn(Optional.of(baseline));
        when(repository.findByPolicyIdAndVersion("PS-001", 2))
                .thenReturn(Optional.of(candidate));
    }

    @Test
    void shouldReplayAllThreeAttackCategoriesUsingPersistedVersions() {
        List<AttackReplayResult> results = service.replay("PS-001", 1, 2);

        assertEquals(3, results.size());
        assertEquals(List.of(
                AttackType.THRESHOLD_SPLITTING,
                AttackType.CUMULATIVE_EXPOSURE,
                AttackType.DUPLICATE_REQUEST),
                results.stream().map(AttackReplayResult::attackType).toList());
        assertTrue(results.stream().allMatch(result ->
                result.baselineVersion() == 1 && result.candidateVersion() == 2));
    }

    @Test
    void shouldMeasureActualApprovedAndBlockedRequestsForBothVersions() {
        List<AttackReplayResult> results = service.replay("PS-001", 1, 2);

        AttackReplayResult threshold = results.get(0);
        assertEquals(5, threshold.baselineAutoApprovedRequests());
        assertEquals(1, threshold.baselineBlockedRequests());
        assertEquals(1, threshold.candidateAutoApprovedRequests());
        assertEquals(5, threshold.candidateBlockedRequests());

        AttackReplayResult cumulative = results.get(1);
        assertEquals(4, cumulative.baselineAutoApprovedRequests());
        assertEquals(0, cumulative.baselineBlockedRequests());
        assertEquals(3, cumulative.candidateAutoApprovedRequests());
        assertEquals(1, cumulative.candidateBlockedRequests());

        AttackReplayResult duplicate = results.get(2);
        assertEquals(1, duplicate.baselineAutoApprovedRequests());
        assertEquals(2, duplicate.baselineBlockedRequests());
        assertEquals(1, duplicate.candidateAutoApprovedRequests());
        assertEquals(2, duplicate.candidateBlockedRequests());
    }

    @Test
    void shouldOnlyClaimNewRemediationWhenCandidateAddsMitigation() {
        List<AttackReplayResult> results = service.replay("PS-001", 1, 2);

        assertTrue(results.get(0).baselineMitigated());
        assertTrue(results.get(0).candidateMitigated());
        assertTrue(results.get(0).remediationVerified());

        assertFalse(results.get(1).baselineMitigated());
        assertTrue(results.get(1).candidateMitigated());
        assertTrue(results.get(1).remediationVerified());

        assertTrue(results.get(2).baselineMitigated());
        assertTrue(results.get(2).candidateMitigated());
        assertFalse(results.get(2).remediationVerified());
    }

    @Test
    void replayLatestPairShouldUseNewestPersistedNonRejectedVersions() {
        when(repository.findAllByPolicyIdOrderByVersionDesc("PS-001"))
                .thenReturn(List.of(candidate, baseline));

        List<AttackReplayResult> results = service.replayLatestPair("PS-001");

        assertEquals(3, results.size());
        verify(repository).findByPolicyIdAndVersion("PS-001", 1);
        verify(repository).findByPolicyIdAndVersion("PS-001", 2);
    }

    @Test
    void shouldRejectCandidateVersionNotGreaterThanBaseline() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.replay("PS-001", 2, 1));
    }

    @Test
    void rejectedVersionCannotBeUsedForReplay() {
        candidate.reject("reviewer", "Not acceptable", java.time.Instant.now());

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.replay("PS-001", 1, 2));
        assertEquals(PolicyVersionStatus.REJECTED, candidate.getStatus());
    }
}
