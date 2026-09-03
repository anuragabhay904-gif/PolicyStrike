package com.refundshield.recoveryagent.repository;

import com.refundshield.recoveryagent.entity.RecoveryAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryActionRepository extends JpaRepository<RecoveryAction, Long> {
    Optional<RecoveryAction> findByOpportunityId(Long opportunityId);
    boolean existsByOpportunityId(Long opportunityId);
    List<RecoveryAction> findByStatus(String status);
}
