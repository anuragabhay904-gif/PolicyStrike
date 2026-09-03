package com.refundshield.recoveryagent.service;

import com.refundshield.recoveryagent.dto.AiAnalysisDTO;
import com.refundshield.recoveryagent.dto.ApproveRecoveryResponse;
import com.refundshield.recoveryagent.dto.OpportunityDTO;
import com.refundshield.recoveryagent.entity.Customer;
import com.refundshield.recoveryagent.entity.Opportunity;
import com.refundshield.recoveryagent.entity.RecoveryAction;
import com.refundshield.recoveryagent.entity.Transaction;
import com.refundshield.recoveryagent.exception.InvalidActionException;
import com.refundshield.recoveryagent.exception.PaymentGatewayException;
import com.refundshield.recoveryagent.exception.ResourceNotFoundException;
import com.refundshield.recoveryagent.repository.CustomerRepository;
import com.refundshield.recoveryagent.repository.OpportunityRepository;
import com.refundshield.recoveryagent.repository.RecoveryActionRepository;
import com.refundshield.recoveryagent.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpportunityService {

    private static final Logger logger = LoggerFactory.getLogger(OpportunityService.class);

    private final OpportunityRepository opportunityRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final RecoveryActionRepository recoveryActionRepository;
    private final AiRecommendationService aiRecommendationService;
    private final RazorpayService razorpayService;
    private final AuditService auditService;

    public OpportunityService(OpportunityRepository opportunityRepository,
                              CustomerRepository customerRepository,
                              TransactionRepository transactionRepository,
                              RecoveryActionRepository recoveryActionRepository,
                              AiRecommendationService aiRecommendationService,
                              RazorpayService razorpayService,
                              AuditService auditService) {
        this.opportunityRepository = opportunityRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.recoveryActionRepository = recoveryActionRepository;
        this.aiRecommendationService = aiRecommendationService;
        this.razorpayService = razorpayService;
        this.auditService = auditService;
    }

    public List<OpportunityDTO> listOpportunities(String status) {
        List<Opportunity> list;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            list = opportunityRepository.findByStatus(status.toUpperCase());
        } else {
            list = opportunityRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public OpportunityDTO getOpportunity(Long id) {
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with ID: " + id));
        return toDTO(opp);
    }

    @Transactional
    public OpportunityDTO analyzeOpportunity(Long id) {
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with ID: " + id));

        Customer customer = customerRepository.findById(opp.getCustomerId()).orElse(null);
        Transaction transaction = opp.getTransactionId() != null
                ? transactionRepository.findById(opp.getTransactionId()).orElse(null)
                : null;

        List<Transaction> history = customer != null
                ? transactionRepository.findByCustomerId(customer.getId())
                : List.of();

        AiAnalysisDTO analysis = aiRecommendationService.generateRecommendation(opp, customer, transaction, history);

        opp.setAiExplanation(analysis.getReason());
        opp.setAiRecommendation(analysis.getRecommendation());
        opp.setSuggestedMessage(analysis.getMessage());
        opp.setConfidence(analysis.getConfidence());
        opp.setUpdatedAt(LocalDateTime.now());

        if ("NEW".equalsIgnoreCase(opp.getStatus())) {
            opp.setStatus("REVIEWED");
        }

        Opportunity saved = opportunityRepository.save(opp);

        auditService.log(saved.getId(), "AI_ANALYSIS_GENERATED", "AI_AGENT",
                String.format("AI generated recovery analysis for opportunity #%d with %s confidence.",
                        saved.getId(), analysis.getConfidence()));

        return toDTO(saved);
    }

    @Transactional(noRollbackFor = PaymentGatewayException.class)
    public ApproveRecoveryResponse approveOpportunity(Long id) {
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with ID: " + id));

        // 1. Validate status
        if ("DISMISSED".equalsIgnoreCase(opp.getStatus())) {
            throw new InvalidActionException("Cannot approve a dismissed opportunity.");
        }
        if ("RECOVERED".equalsIgnoreCase(opp.getStatus())) {
            throw new InvalidActionException("Opportunity has already been recovered.");
        }
        if ("RECOVERY_CREATED".equalsIgnoreCase(opp.getStatus())) {
            throw new InvalidActionException("Recovery action is already active for this opportunity.");
        }
        if (!"NEW".equalsIgnoreCase(opp.getStatus()) && !"REVIEWED".equalsIgnoreCase(opp.getStatus()) && !"FAILED".equalsIgnoreCase(opp.getStatus())) {
            throw new InvalidActionException("Opportunity status '" + opp.getStatus() + "' is not eligible for approval.");
        }

        // 2. Validate customer
        Customer customer = customerRepository.findById(opp.getCustomerId())
                .orElseThrow(() -> new InvalidActionException("Associated customer #" + opp.getCustomerId() + " does not exist."));

        // 3. Validate amount
        if (opp.getAmount() == null || opp.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidActionException("Invalid recovery amount: Must be greater than zero.");
        }

        // 4. Validate no existing successful recovery action
        if (recoveryActionRepository.existsByOpportunityId(opp.getId())) {
            RecoveryAction existing = recoveryActionRepository.findByOpportunityId(opp.getId()).orElse(null);
            if (existing != null && !"FAILED".equalsIgnoreCase(existing.getStatus())) {
                throw new InvalidActionException("Recovery action already created with reference: " + existing.getRazorpayReference());
            }
        }

        auditService.log(opp.getId(), "RECOVERY_APPROVED", "MERCHANT",
                String.format("Merchant approved recovery action for ₹%s (Customer: %s)",
                        opp.getAmount().toPlainString(), customer.getName()));

        RecoveryAction action = recoveryActionRepository.findByOpportunityId(opp.getId())
                .orElse(new RecoveryAction());
        action.setOpportunityId(opp.getId());
        action.setCustomerId(customer.getId());
        action.setAmount(opp.getAmount());
        action.setActionType("PAYMENT_LINK");
        action.setStatus("PENDING");

        try {
            RazorpayService.PaymentLinkResult linkResult = razorpayService.createPaymentLink(opp, customer);

            action.setStatus(linkResult.getStatus());
            action.setPaymentLink(linkResult.getPaymentLink());
            action.setRazorpayReference(linkResult.getRazorpayReference());
            action.setFailureReason(null);
            RecoveryAction savedAction = recoveryActionRepository.save(action);

            opp.setStatus("RECOVERY_CREATED");
            opp.setUpdatedAt(LocalDateTime.now());
            opportunityRepository.save(opp);

            auditService.log(opp.getId(), "PAYMENT_LINK_CREATED", "SYSTEM",
                    String.format("Created Razorpay payment link %s (Ref: %s)",
                            linkResult.getPaymentLink(), linkResult.getRazorpayReference()));

            return new ApproveRecoveryResponse(
                    true,
                    "Payment link generated successfully. Customer can complete payment via Razorpay.",
                    opp.getId(),
                    savedAction.getId(),
                    opp.getAmount(),
                    linkResult.getPaymentLink(),
                    linkResult.getRazorpayReference(),
                    "RECOVERY_CREATED"
            );

        } catch (PaymentGatewayException pge) {
            logger.error("Failed to generate payment link for opportunity #{}", opp.getId(), pge);

            action.setStatus("FAILED");
            action.setFailureReason(pge.getMessage());
            recoveryActionRepository.save(action);

            opp.setStatus("FAILED");
            opp.setUpdatedAt(LocalDateTime.now());
            opportunityRepository.save(opp);

            auditService.log(opp.getId(), "PAYMENT_LINK_FAILED", "SYSTEM",
                    "Payment link creation failed: " + pge.getMessage());

            throw pge;
        }
    }

    @Transactional
    public OpportunityDTO dismissOpportunity(Long id) {
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with ID: " + id));

        opp.setStatus("DISMISSED");
        opp.setUpdatedAt(LocalDateTime.now());
        Opportunity saved = opportunityRepository.save(opp);

        auditService.log(saved.getId(), "RECOVERY_DISMISSED", "MERCHANT",
                String.format("Merchant dismissed opportunity #%d of amount ₹%s",
                        saved.getId(), saved.getAmount().toPlainString()));

        return toDTO(saved);
    }

    public OpportunityDTO toDTO(Opportunity opp) {
        OpportunityDTO dto = new OpportunityDTO();
        dto.setId(opp.getId());
        dto.setCustomerId(opp.getCustomerId());
        dto.setTransactionId(opp.getTransactionId());
        dto.setType(opp.getType());
        dto.setAmount(opp.getAmount());
        dto.setRecoveryScore(opp.getRecoveryScore());
        dto.setStatus(opp.getStatus());
        dto.setReason(opp.getReason());
        dto.setAiExplanation(opp.getAiExplanation());
        dto.setAiRecommendation(opp.getAiRecommendation());
        dto.setSuggestedMessage(opp.getSuggestedMessage());
        dto.setConfidence(opp.getConfidence());
        dto.setCreatedAt(opp.getCreatedAt());
        dto.setUpdatedAt(opp.getUpdatedAt());

        customerRepository.findById(opp.getCustomerId()).ifPresent(c -> {
            dto.setCustomerName(c.getName());
            dto.setCustomerEmail(c.getEmail());
            dto.setCustomerPhone(c.getPhone());
            dto.setCustomerTotalOrders(c.getTotalOrders());
            dto.setCustomerTotalSpent(c.getTotalSpent());
        });

        if (opp.getTransactionId() != null) {
            transactionRepository.findById(opp.getTransactionId()).ifPresent(t -> {
                dto.setTransactionStatus(t.getStatus());
                dto.setTransactionType(t.getTransactionType());
                dto.setTransactionDate(t.getTransactionDate());
            });
        }

        recoveryActionRepository.findByOpportunityId(opp.getId()).ifPresent(ra -> {
            dto.setRecoveryActionId(ra.getId());
            dto.setRecoveryActionStatus(ra.getStatus());
            dto.setPaymentLink(ra.getPaymentLink());
            dto.setRazorpayReference(ra.getRazorpayReference());
            dto.setFailureReason(ra.getFailureReason());
        });

        return dto;
    }
}
