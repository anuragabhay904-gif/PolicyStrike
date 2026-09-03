package com.refundshield.recoveryagent.controller;

import com.refundshield.recoveryagent.dto.DashboardDTO;
import com.refundshield.recoveryagent.dto.OpportunityDTO;
import com.refundshield.recoveryagent.entity.Opportunity;
import com.refundshield.recoveryagent.entity.RecoveryAction;
import com.refundshield.recoveryagent.entity.Transaction;
import com.refundshield.recoveryagent.repository.CustomerRepository;
import com.refundshield.recoveryagent.repository.OpportunityRepository;
import com.refundshield.recoveryagent.repository.RecoveryActionRepository;
import com.refundshield.recoveryagent.repository.TransactionRepository;
import com.refundshield.recoveryagent.service.AuditService;
import com.refundshield.recoveryagent.service.OpportunityService;
import com.refundshield.recoveryagent.service.RazorpayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final OpportunityRepository opportunityRepository;
    private final RecoveryActionRepository recoveryActionRepository;
    private final OpportunityService opportunityService;
    private final AuditService auditService;
    private final RazorpayService razorpayService;

    public DashboardController(CustomerRepository customerRepository,
                               TransactionRepository transactionRepository,
                               OpportunityRepository opportunityRepository,
                               RecoveryActionRepository recoveryActionRepository,
                               OpportunityService opportunityService,
                               AuditService auditService,
                               RazorpayService razorpayService) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.opportunityRepository = opportunityRepository;
        this.recoveryActionRepository = recoveryActionRepository;
        this.opportunityService = opportunityService;
        this.auditService = auditService;
        this.razorpayService = razorpayService;
    }

    @GetMapping
    public DashboardDTO getDashboard() {
        DashboardDTO dto = new DashboardDTO();

        List<Transaction> transactions = transactionRepository.findAll();
        List<Opportunity> opportunities = opportunityRepository.findAll();
        List<RecoveryAction> recoveryActions = recoveryActionRepository.findAll();

        BigDecimal successRev = transactions.stream()
                .filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal failedRev = transactions.stream()
                .filter(t -> "FAILED".equalsIgnoreCase(t.getStatus()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal abandonedRev = transactions.stream()
                .filter(t -> "ABANDONED".equalsIgnoreCase(t.getStatus()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Potential recoverable: sum of opportunities with NEW, REVIEWED, APPROVED
        BigDecimal potentialRecoverable = opportunities.stream()
                .filter(o -> "NEW".equalsIgnoreCase(o.getStatus()) || "REVIEWED".equalsIgnoreCase(o.getStatus()) || "APPROVED".equalsIgnoreCase(o.getStatus()))
                .map(Opportunity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeCount = opportunities.stream()
                .filter(o -> "NEW".equalsIgnoreCase(o.getStatus()) || "REVIEWED".equalsIgnoreCase(o.getStatus()) || "APPROVED".equalsIgnoreCase(o.getStatus()))
                .count();

        // Recovered revenue: successful or created recovery actions
        BigDecimal recovered = recoveryActions.stream()
                .filter(ra -> "CREATED".equalsIgnoreCase(ra.getStatus()) || "PAID".equalsIgnoreCase(ra.getStatus()))
                .map(RecoveryAction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setTotalRevenue(successRev);
        dto.setPotentialRecoverableRevenue(potentialRecoverable);
        dto.setActiveOpportunitiesCount(activeCount);
        dto.setRecoveredRevenue(recovered);

        dto.setSuccessfulRevenue(successRev);
        dto.setFailedRevenue(failedRev);
        dto.setAbandonedRevenue(abandonedRev);

        dto.setTotalCustomers(customerRepository.count());
        dto.setTotalTransactions(transactions.size());
        dto.setPaymentMode(razorpayService.getPaymentMode());
        dto.setFailureSimulationActive(razorpayService.isSimulateFailure());

        // Top 5 recent opportunities
        List<OpportunityDTO> recentOpps = opportunityRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(5)
                .map(opportunityService::toDTO)
                .collect(Collectors.toList());
        dto.setRecentOpportunities(recentOpps);

        // Top 10 recent activities
        dto.setRecentActivities(auditService.getRecentAuditEvents());

        return dto;
    }
}
