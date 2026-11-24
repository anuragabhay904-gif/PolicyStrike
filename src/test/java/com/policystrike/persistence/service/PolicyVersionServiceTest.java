package com.policystrike.persistence.service;

import com.policystrike.api.dto.CreatePolicyVersionRequest;
import com.policystrike.api.dto.PolicyVersionResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class PolicyVersionServiceTest {

    private PolicyVersionRepository repository;
    private PolicyVersionService service;
    private PolicyVersionEntity activeV1;

    @BeforeEach
    void setUp() {
        repository = mock(PolicyVersionRepository.class);
        service = new PolicyVersionService(repository);
        activeV1 = PolicyVersionEntity.from(new RefundPolicy(
                "PS-001", 1,
                new BigDecimal("7500"),
                new BigDecimal("20000"),
                Duration.ofHours(24), Duration.ofDays(30), true));
        when(repository.save(any(PolicyVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void newVersionMustRemainPendingUntilHumanApproval() {
        when(repository.existsByPolicyId("PS-001")).thenReturn(true);
        when(repository.existsByPolicyIdAndStatus(
                "PS-001", PolicyVersionStatus.PENDING_APPROVAL)).thenReturn(false);
        when(repository.findFirstByPolicyIdOrderByVersionDesc("PS-001"))
                .thenReturn(Optional.of(activeV1));

        PolicyVersionResponse response = service.createNextVersion(
                "PS-001",
                new CreatePolicyVersionRequest(
                        new BigDecimal("2000"),
                        new BigDecimal("5000"),
                        24L,
                        30L,
                        true));

        assertEquals(2, response.version());
        assertEquals(PolicyVersionStatus.PENDING_APPROVAL, response.status());
        assertNull(response.approvedBy());
        assertNull(response.approvedAt());
        assertEquals(PolicyVersionStatus.ACTIVE, activeV1.getStatus());
    }

    @Test
    void approvalActivatesCandidateAndSupersedesPreviousActiveVersion() {
        PolicyVersionEntity pendingV2 = pendingV2();
        when(repository.findByPolicyIdAndVersion("PS-001", 2))
                .thenReturn(Optional.of(pendingV2));
        when(repository.findAllByPolicyIdAndStatusOrderByVersionDesc(
                "PS-001", PolicyVersionStatus.ACTIVE))
                .thenReturn(List.of(activeV1));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyVersionResponse response = service.approveVersion(
                "PS-001", 2, "risk.manager@example.com");

        assertEquals(PolicyVersionStatus.SUPERSEDED, activeV1.getStatus());
        assertEquals(PolicyVersionStatus.ACTIVE, pendingV2.getStatus());
        assertEquals("risk.manager@example.com", response.approvedBy());
        assertNotNull(response.approvedAt());
    }

    @Test
    void rejectedProposalDoesNotReplaceActiveVersion() {
        PolicyVersionEntity pendingV2 = pendingV2();
        when(repository.findByPolicyIdAndVersion("PS-001", 2))
                .thenReturn(Optional.of(pendingV2));

        PolicyVersionResponse response = service.rejectVersion(
                "PS-001", 2, "risk.manager@example.com", "Limit values need revision");

        assertEquals(PolicyVersionStatus.REJECTED, response.status());
        assertEquals("Limit values need revision", response.rejectionReason());
        assertEquals(PolicyVersionStatus.ACTIVE, activeV1.getStatus());
        verify(repository, never()).findAllByPolicyIdAndStatusOrderByVersionDesc(
                "PS-001", PolicyVersionStatus.ACTIVE);
    }

    @Test
    void cannotCreateSecondPendingProposalForSamePolicy() {
        when(repository.existsByPolicyId("PS-001")).thenReturn(true);
        when(repository.existsByPolicyIdAndStatus(
                "PS-001", PolicyVersionStatus.PENDING_APPROVAL)).thenReturn(true);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.createNextVersion("PS-001", new CreatePolicyVersionRequest(
                        new BigDecimal("2000"), new BigDecimal("5000"), 24L, 30L, true)));
    }

    private PolicyVersionEntity pendingV2() {
        return PolicyVersionEntity.pending(new RefundPolicy(
                "PS-001", 2,
                new BigDecimal("2000"),
                new BigDecimal("5000"),
                Duration.ofHours(24), Duration.ofDays(30), true));
    }
}
