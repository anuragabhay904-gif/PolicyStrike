package com.refundshield.recoveryagent.controller;

import com.refundshield.recoveryagent.entity.AuditEvent;
import com.refundshield.recoveryagent.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditEvent> getAuditEvents(@RequestParam(required = false) Long opportunityId) {
        if (opportunityId != null) {
            return auditService.getEventsForOpportunity(opportunityId);
        }
        return auditService.getAllAuditEvents();
    }
}
