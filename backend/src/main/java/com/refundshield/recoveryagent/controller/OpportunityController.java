package com.refundshield.recoveryagent.controller;

import com.refundshield.recoveryagent.dto.ApproveRecoveryResponse;
import com.refundshield.recoveryagent.dto.OpportunityDTO;
import com.refundshield.recoveryagent.entity.Opportunity;
import com.refundshield.recoveryagent.service.OpportunityService;
import com.refundshield.recoveryagent.service.RevenueLeakDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final RevenueLeakDetectionService revenueLeakDetectionService;

    public OpportunityController(OpportunityService opportunityService,
                                 RevenueLeakDetectionService revenueLeakDetectionService) {
        this.opportunityService = opportunityService;
        this.revenueLeakDetectionService = revenueLeakDetectionService;
    }

    @GetMapping
    public List<OpportunityDTO> listOpportunities(@RequestParam(required = false) String status) {
        return opportunityService.listOpportunities(status);
    }

    @GetMapping("/{id}")
    public OpportunityDTO getOpportunity(@PathVariable Long id) {
        return opportunityService.getOpportunity(id);
    }

    @PostMapping("/{id}/analyze")
    public OpportunityDTO analyzeOpportunity(@PathVariable Long id) {
        return opportunityService.analyzeOpportunity(id);
    }

    @PostMapping("/{id}/approve")
    public ApproveRecoveryResponse approveOpportunity(@PathVariable Long id) {
        return opportunityService.approveOpportunity(id);
    }

    @PostMapping("/{id}/dismiss")
    public OpportunityDTO dismissOpportunity(@PathVariable Long id) {
        return opportunityService.dismissOpportunity(id);
    }

    @PostMapping("/detect")
    public List<OpportunityDTO> triggerDetection() {
        List<Opportunity> newOpps = revenueLeakDetectionService.detectAllLeaks();
        return newOpps.stream().map(opportunityService::toDTO).collect(Collectors.toList());
    }
}
