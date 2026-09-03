package com.refundshield.recoveryagent.service;

import com.refundshield.recoveryagent.entity.Customer;
import com.refundshield.recoveryagent.entity.Opportunity;
import com.refundshield.recoveryagent.entity.Transaction;
import com.refundshield.recoveryagent.repository.CustomerRepository;
import com.refundshield.recoveryagent.repository.OpportunityRepository;
import com.refundshield.recoveryagent.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class RevenueLeakDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(RevenueLeakDetectionService.class);

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final OpportunityRepository opportunityRepository;
    private final RecoveryScoreCalculator recoveryScoreCalculator;
    private final AuditService auditService;

    @Value("${app.leak-detection.high-value-threshold:500.00}")
    private BigDecimal highValueThreshold;

    @Value("${app.leak-detection.inactive-days-threshold:30}")
    private int inactiveDaysThreshold;

    @Value("${app.leak-detection.repeat-customer-min-spent:1000.00}")
    private BigDecimal repeatCustomerMinSpent;

    public RevenueLeakDetectionService(CustomerRepository customerRepository,
                                       TransactionRepository transactionRepository,
                                       OpportunityRepository opportunityRepository,
                                       RecoveryScoreCalculator recoveryScoreCalculator,
                                       AuditService auditService) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.opportunityRepository = opportunityRepository;
        this.recoveryScoreCalculator = recoveryScoreCalculator;
        this.auditService = auditService;
    }

    @Transactional
    public List<Opportunity> detectAllLeaks() {
        List<Opportunity> newOpportunities = new ArrayList<>();

        List<Customer> allCustomers = customerRepository.findAll();
        List<Transaction> allTransactions = transactionRepository.findAll();

        // 1. Detect FAILED_PAYMENT_RECOVERY
        for (Transaction tx : allTransactions) {
            if ("FAILED".equalsIgnoreCase(tx.getStatus()) && !opportunityRepository.existsByTransactionId(tx.getId())) {
                Customer customer = customerRepository.findById(tx.getCustomerId()).orElse(null);
                if (customer != null && tx.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    long failedCount = transactionRepository.countByCustomerIdAndStatus(customer.getId(), "FAILED");
                    int score = recoveryScoreCalculator.calculateScore("FAILED_PAYMENT_RECOVERY", customer, tx, failedCount);

                    String reason = String.format("Failed payment of ₹%s detected on %s gateway attempt. Customer has %d prior completed orders.",
                            tx.getAmount().toPlainString(),
                            tx.getTransactionType() != null ? tx.getTransactionType() : "standard",
                            customer.getTotalOrders());

                    Opportunity opp = new Opportunity(customer.getId(), tx.getId(), "FAILED_PAYMENT_RECOVERY", tx.getAmount(), score, "NEW", reason);
                    Opportunity saved = opportunityRepository.save(opp);
                    newOpportunities.add(saved);

                    auditService.log(saved.getId(), "OPPORTUNITY_DETECTED", "SYSTEM",
                            String.format("Detected failed payment leak of ₹%s for customer %s (Score: %d)",
                                    tx.getAmount().toPlainString(), customer.getName(), score));
                }
            }
        }

        // 2. Detect HIGH_VALUE_CHECKOUT_RECOVERY
        for (Transaction tx : allTransactions) {
            if ("ABANDONED".equalsIgnoreCase(tx.getStatus()) &&
                    tx.getAmount().compareTo(highValueThreshold) >= 0 &&
                    !opportunityRepository.existsByTransactionId(tx.getId())) {

                Customer customer = customerRepository.findById(tx.getCustomerId()).orElse(null);
                if (customer != null) {
                    long failedCount = transactionRepository.countByCustomerIdAndStatus(customer.getId(), "FAILED");
                    int score = recoveryScoreCalculator.calculateScore("HIGH_VALUE_CHECKOUT_RECOVERY", customer, tx, failedCount);

                    String reason = String.format("High-value checkout attempt of ₹%s was abandoned before authorization. High recovery potential.",
                            tx.getAmount().toPlainString());

                    Opportunity opp = new Opportunity(customer.getId(), tx.getId(), "HIGH_VALUE_CHECKOUT_RECOVERY", tx.getAmount(), score, "NEW", reason);
                    Opportunity saved = opportunityRepository.save(opp);
                    newOpportunities.add(saved);

                    auditService.log(saved.getId(), "OPPORTUNITY_DETECTED", "SYSTEM",
                            String.format("Detected high-value abandoned checkout leak of ₹%s for customer %s (Score: %d)",
                                    tx.getAmount().toPlainString(), customer.getName(), score));
                }
            }
        }

        // 3. Detect REPEAT_CUSTOMER_REENGAGEMENT
        for (Customer customer : allCustomers) {
            if (customer.getTotalOrders() != null && customer.getTotalOrders() > 0 &&
                    customer.getTotalSpent() != null && customer.getTotalSpent().compareTo(repeatCustomerMinSpent) >= 0 &&
                    customer.getLastOrderDate() != null) {

                long daysInactive = ChronoUnit.DAYS.between(customer.getLastOrderDate(), LocalDateTime.now());
                if (daysInactive >= inactiveDaysThreshold) {
                    boolean alreadyActive = opportunityRepository.existsByCustomerIdAndTypeAndStatusNot(
                            customer.getId(), "REPEAT_CUSTOMER_REENGAGEMENT", "DISMISSED");

                    if (!alreadyActive) {
                        BigDecimal avgOrder = customer.getTotalSpent()
                                .divide(BigDecimal.valueOf(customer.getTotalOrders()), 2, RoundingMode.HALF_UP);

                        int score = recoveryScoreCalculator.calculateScore("REPEAT_CUSTOMER_REENGAGEMENT", customer, null, 0);

                        String reason = String.format("High-value customer (%s orders, ₹%s lifetime spend) inactive for %d days.",
                                customer.getTotalOrders(), customer.getTotalSpent().toPlainString(), daysInactive);

                        Opportunity opp = new Opportunity(customer.getId(), null, "REPEAT_CUSTOMER_REENGAGEMENT", avgOrder, score, "NEW", reason);
                        Opportunity saved = opportunityRepository.save(opp);
                        newOpportunities.add(saved);

                        auditService.log(saved.getId(), "OPPORTUNITY_DETECTED", "SYSTEM",
                                String.format("Detected repeat customer drop-off leak for customer %s (Score: %d, Estimated Value: ₹%s)",
                                        customer.getName(), score, avgOrder.toPlainString()));
                    }
                }
            }
        }

        logger.info("Leak detection completed. Found {} new opportunities.", newOpportunities.size());
        return newOpportunities;
    }
}
