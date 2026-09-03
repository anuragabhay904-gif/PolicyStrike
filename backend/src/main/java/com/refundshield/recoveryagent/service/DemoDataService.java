package com.refundshield.recoveryagent.service;

import com.refundshield.recoveryagent.dto.OpportunityDTO;
import com.refundshield.recoveryagent.dto.SimulateLeakRequest;
import com.refundshield.recoveryagent.entity.Customer;
import com.refundshield.recoveryagent.entity.Opportunity;
import com.refundshield.recoveryagent.entity.Transaction;
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
import java.util.Random;
import java.util.UUID;

@Service
public class DemoDataService {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataService.class);

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final OpportunityRepository opportunityRepository;
    private final RecoveryActionRepository recoveryActionRepository;
    private final RevenueLeakDetectionService revenueLeakDetectionService;
    private final OpportunityService opportunityService;
    private final AuditService auditService;
    private final RazorpayService razorpayService;

    public DemoDataService(CustomerRepository customerRepository,
                           TransactionRepository transactionRepository,
                           OpportunityRepository opportunityRepository,
                           RecoveryActionRepository recoveryActionRepository,
                           RevenueLeakDetectionService revenueLeakDetectionService,
                           OpportunityService opportunityService,
                           AuditService auditService,
                           RazorpayService razorpayService) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.opportunityRepository = opportunityRepository;
        this.recoveryActionRepository = recoveryActionRepository;
        this.revenueLeakDetectionService = revenueLeakDetectionService;
        this.opportunityService = opportunityService;
        this.auditService = auditService;
        this.razorpayService = razorpayService;
    }

    @Transactional
    public void seedDemoData() {
        logger.info("Seeding demo customers and transactions...");

        // Clean existing test data if desired or keep
        recoveryActionRepository.deleteAll();
        opportunityRepository.deleteAll();
        transactionRepository.deleteAll();
        customerRepository.deleteAll();

        // 10 realistic Indian merchant customers
        Customer c1 = customerRepository.save(new Customer("Rahul Sharma", "rahul.sharma@example.com", "+919876543210", 4, new BigDecimal("14500.00"), LocalDateTime.now().minusDays(3)));
        Customer c2 = customerRepository.save(new Customer("Priya Patel", "priya.patel@example.com", "+919812345678", 2, new BigDecimal("3200.00"), LocalDateTime.now().minusDays(2)));
        Customer c3 = customerRepository.save(new Customer("Amitav Verma", "amitav.v@example.com", "+919711223344", 6, new BigDecimal("28900.00"), LocalDateTime.now().minusDays(45))); // Inactive repeat
        Customer c4 = customerRepository.save(new Customer("Sneha Kulkarni", "sneha.k@example.com", "+919922334455", 1, new BigDecimal("1200.00"), LocalDateTime.now().minusDays(1)));
        Customer c5 = customerRepository.save(new Customer("Vikram Malhotra", "vikram.m@example.com", "+919833445566", 5, new BigDecimal("19800.00"), LocalDateTime.now().minusDays(60))); // Inactive repeat
        Customer c6 = customerRepository.save(new Customer("Ananya Iyer", "ananya.iyer@example.com", "+919844556677", 3, new BigDecimal("8750.00"), LocalDateTime.now().minusDays(5)));
        Customer c7 = customerRepository.save(new Customer("Rohan Gupta", "rohan.gupta@example.com", "+919855667788", 1, new BigDecimal("650.00"), LocalDateTime.now().minusDays(12)));
        Customer c8 = customerRepository.save(new Customer("Deepika Rao", "deepika.rao@example.com", "+919866778899", 7, new BigDecimal("34500.00"), LocalDateTime.now().minusDays(80))); // Inactive repeat
        Customer c9 = customerRepository.save(new Customer("Karan Mehta", "karan.mehta@example.com", "+919877889900", 2, new BigDecimal("4100.00"), LocalDateTime.now().minusDays(4)));
        Customer c10 = customerRepository.save(new Customer("Neha Singhal", "neha.singhal@example.com", "+919888990011", 1, new BigDecimal("890.00"), LocalDateTime.now().minusDays(20)));

        // 25+ Transactions across channels & statuses
        // Customer 1: Rahul Sharma (Successful past, recent failed)
        transactionRepository.save(new Transaction(c1.getId(), new BigDecimal("3500.00"), "SUCCESS", "CARD", LocalDateTime.now().minusDays(40), "ord_101"));
        transactionRepository.save(new Transaction(c1.getId(), new BigDecimal("4200.00"), "SUCCESS", "UPI", LocalDateTime.now().minusDays(20), "ord_102"));
        transactionRepository.save(new Transaction(c1.getId(), new BigDecimal("2499.00"), "FAILED", "CARD", LocalDateTime.now().minusHours(4), "ord_103")); // Leak candidate

        // Customer 2: Priya Patel (Successful past, recent failed)
        transactionRepository.save(new Transaction(c2.getId(), new BigDecimal("1800.00"), "SUCCESS", "UPI", LocalDateTime.now().minusDays(15), "ord_104"));
        transactionRepository.save(new Transaction(c2.getId(), new BigDecimal("1400.00"), "SUCCESS", "UPI", LocalDateTime.now().minusDays(2), "ord_105"));
        transactionRepository.save(new Transaction(c2.getId(), new BigDecimal("1250.00"), "FAILED", "UPI", LocalDateTime.now().minusHours(2), "ord_106")); // Leak candidate

        // Customer 3: Amitav Verma (Inactive high value repeat)
        transactionRepository.save(new Transaction(c3.getId(), new BigDecimal("8900.00"), "SUCCESS", "NETBANKING", LocalDateTime.now().minusDays(90), "ord_107"));
        transactionRepository.save(new Transaction(c3.getId(), new BigDecimal("11000.00"), "SUCCESS", "CARD", LocalDateTime.now().minusDays(65), "ord_108"));
        transactionRepository.save(new Transaction(c3.getId(), new BigDecimal("9000.00"), "SUCCESS", "CARD", LocalDateTime.now().minusDays(45), "ord_109"));

        // Customer 4: Sneha Kulkarni (High value abandoned checkout)
        transactionRepository.save(new Transaction(c4.getId(), new BigDecimal("1200.00"), "SUCCESS", "UPI", LocalDateTime.now().minusDays(10), "ord_110"));
        transactionRepository.save(new Transaction(c4.getId(), new BigDecimal("7499.00"), "ABANDONED", "CARD", LocalDateTime.now().minusHours(6), "ord_111")); // High value abandoned

        // Customer 5: Vikram Malhotra (Dormant repeat)
        transactionRepository.save(new Transaction(c5.getId(), new BigDecimal("9800.00"), "SUCCESS", "CARD", LocalDateTime.now().minusDays(95), "ord_112"));
        transactionRepository.save(new Transaction(c5.getId(), new BigDecimal("10000.00"), "SUCCESS", "CARD", LocalDateTime.now().minusDays(60), "ord_113"));

        // Customer 6: Ananya Iyer (Multiple failed payment attempts)
        transactionRepository.save(new Transaction(c6.getId(), new BigDecimal("4500.00"), "SUCCESS", "UPI", LocalDateTime.now().minusDays(25), "ord_114"));
        transactionRepository.save(new Transaction(c6.getId(), new BigDecimal("4250.00"), "SUCCESS", "UPI", LocalDateTime.now().minusDays(5), "ord_115"));
        transactionRepository.save(new Transaction(c6.getId(), new BigDecimal("3899.00"), "FAILED", "CARD", LocalDateTime.now().minusHours(12), "ord_116"));
        transactionRepository.save(new Transaction(c6.getId(), new BigDecimal("3899.00"), "FAILED", "UPI", LocalDateTime.now().minusHours(11), "ord_117")); // Repeat failure!

        // Customer 7: Rohan Gupta (Abandoned checkout)
        transactionRepository.save(new Transaction(c7.getId(), new BigDecimal("650.00"), "SUCCESS", "UPI", LocalDateTime.now().minusDays(12), "ord_118"));
        transactionRepository.save(new Transaction(c7.getId(), new BigDecimal("1850.00"), "ABANDONED", "NETBANKING", LocalDateTime.now().minusHours(8), "ord_119"));

        // Customer 8: Deepika Rao (High-value dormant customer)
        transactionRepository.save(new Transaction(c8.getId(), new BigDecimal("15000.00"), "SUCCESS", "CARD", LocalDateTime.now().minusDays(120), "ord_120"));
        transactionRepository.save(new Transaction(c8.getId(), new BigDecimal("19500.00"), "SUCCESS", "CARD", LocalDateTime.now().minusDays(80), "ord_121"));

        // Customer 9: Karan Mehta (Successful + high value abandoned)
        transactionRepository.save(new Transaction(c9.getId(), new BigDecimal("2100.00"), "SUCCESS", "UPI", LocalDateTime.now().minusDays(14), "ord_122"));
        transactionRepository.save(new Transaction(c9.getId(), new BigDecimal("2000.00"), "SUCCESS", "CARD", LocalDateTime.now().minusDays(4), "ord_123"));
        transactionRepository.save(new Transaction(c9.getId(), new BigDecimal("5600.00"), "ABANDONED", "CARD", LocalDateTime.now().minusHours(3), "ord_124"));

        // Customer 10: Neha Singhal (Recent failed)
        transactionRepository.save(new Transaction(c10.getId(), new BigDecimal("890.00"), "SUCCESS", "UPI", LocalDateTime.now().minusDays(20), "ord_125"));
        transactionRepository.save(new Transaction(c10.getId(), new BigDecimal("1499.00"), "FAILED", "UPI", LocalDateTime.now().minusHours(1), "ord_126"));

        auditService.log(null, "DEMO_SEEDED", "SYSTEM", "Seeded 10 demo customers and 26 transactions across multiple channels.");

        // Automatically run detection to populate initial opportunities
        revenueLeakDetectionService.detectAllLeaks();
    }

    @Transactional
    public OpportunityDTO simulateLeak(SimulateLeakRequest request) {
        // Pick or find target customer
        Customer customer;
        if (request != null && request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
        } else {
            List<Customer> customers = customerRepository.findAll();
            if (customers.isEmpty()) {
                seedDemoData();
                customers = customerRepository.findAll();
            }
            // Pick a customer with orders
            customer = customers.stream().filter(c -> c.getTotalOrders() > 0).findFirst().orElse(customers.get(0));
        }

        BigDecimal amount = (request != null && request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0)
                ? request.getAmount()
                : new BigDecimal("4999.00");

        String type = (request != null && request.getType() != null && !request.getType().trim().isEmpty())
                ? request.getType()
                : "FAILED_PAYMENT_RECOVERY";

        String status = "HIGH_VALUE_CHECKOUT_RECOVERY".equalsIgnoreCase(type) ? "ABANDONED" : "FAILED";
        String channel = (new Random().nextBoolean()) ? "CARD" : "UPI";
        String orderId = "sim_" + UUID.randomUUID().toString().substring(0, 8);

        Transaction newTx = new Transaction(customer.getId(), amount, status, channel, LocalDateTime.now(), orderId);
        Transaction savedTx = transactionRepository.save(newTx);

        auditService.log(null, "TRANSACTION_SIMULATED", "SYSTEM",
                String.format("Simulated new %s transaction of ₹%s for customer %s (#%s)",
                        status, amount.toPlainString(), customer.getName(), orderId));

        // Detect leak
        List<Opportunity> created = revenueLeakDetectionService.detectAllLeaks();

        // Return the newly created opportunity or existing for this transaction
        Opportunity opp = opportunityRepository.findByTransactionId(savedTx.getId())
                .orElseGet(() -> !created.isEmpty() ? created.get(0) : null);

        if (opp == null) {
            // If already exists or edge case, fetch latest
            opp = opportunityRepository.findAllByOrderByCreatedAtDesc().stream().findFirst().orElse(null);
        }

        return opp != null ? opportunityService.toDTO(opp) : null;
    }

    public boolean toggleFailureSimulation(Boolean enabled) {
        boolean newState = enabled != null ? enabled : !razorpayService.isSimulateFailure();
        razorpayService.setSimulateFailure(newState);

        auditService.log(null, "GATEWAY_FAILURE_TOGGLED", "MERCHANT",
                "Razorpay failure simulation mode is now " + (newState ? "ENABLED" : "DISABLED"));

        return newState;
    }
}
