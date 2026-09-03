package com.refundshield.recoveryagent.service;

import com.refundshield.recoveryagent.entity.AuditEvent;
import com.refundshield.recoveryagent.repository.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public AuditEvent log(Long opportunityId, String action, String actor, String details) {
        AuditEvent event = new AuditEvent(opportunityId, action, actor, details);
        return auditEventRepository.save(event);
    }

    public List<AuditEvent> getAllAuditEvents() {
        return auditEventRepository.findAllByOrderByTimestampDesc();
    }

    public List<AuditEvent> getRecentAuditEvents() {
        return auditEventRepository.findTop10ByOrderByTimestampDesc();
    }

    public List<AuditEvent> getEventsForOpportunity(Long opportunityId) {
        return auditEventRepository.findByOpportunityIdOrderByTimestampDesc(opportunityId);
    }
}
