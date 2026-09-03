package com.refundshield.recoveryagent.repository;

import com.refundshield.recoveryagent.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findAllByOrderByTimestampDesc();
    List<AuditEvent> findTop10ByOrderByTimestampDesc();
    List<AuditEvent> findByOpportunityIdOrderByTimestampDesc(Long opportunityId);
}
