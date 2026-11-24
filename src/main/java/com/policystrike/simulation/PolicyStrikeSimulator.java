package com.policystrike.simulation;

import com.policystrike.domain.*;
import com.policystrike.engine.RefundPolicyEvaluator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class PolicyStrikeSimulator {

    private static final Instant BASE_TIME =
            Instant.parse("2026-01-01T00:00:00Z");

    private static final int CUSTOMER_COUNT = 100;
    private static final int REQUESTS_PER_CUSTOMER = 10;
    private static final BigDecimal MAX_NORMAL_REQUEST =
            new BigDecimal("250.00");
    private static final BigDecimal NORMAL_AUTO_LIMIT_FACTOR =
            new BigDecimal("0.25");
    private static final BigDecimal NORMAL_CUMULATIVE_LIMIT_DIVISOR =
            new BigDecimal("12");
    private static final BigDecimal THRESHOLD_ATTACK_FACTOR =
            new BigDecimal("0.60");
    private static final BigDecimal CUMULATIVE_ATTACK_AUTO_FACTOR =
            new BigDecimal("0.99");
    private static final BigDecimal CUMULATIVE_ATTACK_LIMIT_FACTOR =
            new BigDecimal("0.90");

    public static void main(String[] args) {
        PolicyStrikeSimulator simulator = new PolicyStrikeSimulator();
        simulator.printReport(simulator.run());
    }

    /** Runs the original PS-001 v1 demonstration policy. */
    public SimulationReport run() {
        RefundPolicy policy = new RefundPolicy(
                "PS-001",
                1,
                new BigDecimal("7500"),
                new BigDecimal("20000"),
                Duration.ofHours(24),
                Duration.ofDays(30),
                true
        );
        return run(policy);
    }

    public SimulationReport run(RefundPolicy policy) {
        return runDetailed(policy).report();
    }

    public SimulationExecution runDetailed(RefundPolicy policy) {
        Objects.requireNonNull(policy, "Policy cannot be null");

        List<SimulationRecord> dataset = generateDataset(policy);
        dataset.sort(Comparator.comparing(
                record -> record.request().getRequestedAt()
        ));

        RefundPolicyEvaluator evaluator = new RefundPolicyEvaluator();
        List<EvaluationRecord> evaluations = new ArrayList<>(dataset.size());

        int approved = 0;
        int manualReview = 0;
        int blocked = 0;
        int falsePositives = 0;
        BigDecimal approvedExposure = BigDecimal.ZERO;
        Set<String> restrictedCustomers = new HashSet<>();

        Map<AttackType, Integer> injectedAttacks =
                new EnumMap<>(AttackType.class);
        Map<AttackType, Integer> detectedAttacks =
                new EnumMap<>(AttackType.class);

        for (SimulationRecord record : dataset) {
            RefundRequest request = record.request();
            RefundDecision decision = evaluator.evaluate(policy, request);

            evaluations.add(new EvaluationRecord(
                    request, decision, record.expectedAttack()
            ));

            switch (decision.getStatus()) {
                case AUTO_APPROVE -> {
                    approved++;
                    approvedExposure = approvedExposure.add(request.getAmount());
                }
                case MANUAL_REVIEW -> manualReview++;
                case BLOCKED -> {
                    blocked++;
                    restrictedCustomers.add(request.getCustomerId());
                }
            }

            AttackType expectedAttack = record.expectedAttack();
            if (expectedAttack != AttackType.NONE) {
                injectedAttacks.merge(expectedAttack, 1, Integer::sum);

                // This benchmark calls an attack "detected" when the evaluator
                // deterministically blocks the designated attack-trigger request.
                if (decision.getStatus() == DecisionStatus.BLOCKED) {
                    detectedAttacks.merge(expectedAttack, 1, Integer::sum);
                }
            } else if (decision.getStatus() == DecisionStatus.BLOCKED) {
                falsePositives++;
            }
        }

        int totalInjected = injectedAttacks.values().stream()
                .mapToInt(Integer::intValue).sum();
        int totalDetected = detectedAttacks.values().stream()
                .mapToInt(Integer::intValue).sum();

        SimulationReport report = new SimulationReport(
                CUSTOMER_COUNT,
                dataset.size(),
                approved,
                manualReview,
                blocked,
                approvedExposure,
                restrictedCustomers.size(),
                injectedAttacks,
                detectedAttacks,
                falsePositives,
                totalInjected - totalDetected
        );

        return new SimulationExecution(report, evaluations);
    }

    /**
     * The benchmark data is calibrated to the policy being tested.
     * Legitimate traffic stays below the active policy's limits, while each
     * injected attack crosses one specific control without first crossing
     * another control.
     */
    private List<SimulationRecord> generateDataset(RefundPolicy policy) {
        List<SimulationRecord> records = new ArrayList<>();
        BigDecimal normalAmount = normalRequestAmount(policy);

        for (int customerNumber = 1;
             customerNumber <= CUSTOMER_COUNT;
             customerNumber++) {

            String customerId = String.format("CUS-%04d", customerNumber);

            if (customerNumber <= 70) {
                generateNormalCustomer(records, customerId, normalAmount);
            } else if (customerNumber <= 80) {
                generateThresholdAttack(records, customerId, policy, normalAmount);
            } else if (customerNumber <= 90) {
                generateCumulativeAttack(records, customerId, policy, normalAmount);
            } else {
                generateDuplicateAttack(records, customerId, normalAmount);
            }
        }

        return records;
    }

    /**
     * At most eight of these requests can fall in the 30-day benchmark window.
     * The INR 250 ceiling keeps the same benign dataset safe under both saved
     * PS-001 versions; the policy-relative caps also support stricter policies.
     */
    private BigDecimal normalRequestAmount(RefundPolicy policy) {
        BigDecimal byIndividualLimit = policy.getAutomaticRefundLimit()
                .multiply(NORMAL_AUTO_LIMIT_FACTOR);
        BigDecimal byCumulativeLimit = policy.getCustomerCumulativeLimit()
                .divide(NORMAL_CUMULATIVE_LIMIT_DIVISOR, 2, RoundingMode.DOWN);

        BigDecimal amount = min(
                MAX_NORMAL_REQUEST,
                byIndividualLimit,
                byCumulativeLimit
        ).setScale(2, RoundingMode.DOWN);

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Policy limits are too small to generate INR-denominated benchmark requests"
            );
        }
        return amount;
    }

    private void generateNormalCustomer(
            List<SimulationRecord> records,
            String customerId,
            BigDecimal amount) {

        for (int i = 0; i < REQUESTS_PER_CUSTOMER; i++) {
            addRequest(
                    records,
                    customerId,
                    i,
                    "TX-" + customerId + "-" + i,
                    amount,
                    BASE_TIME.plus(Duration.ofDays(i * 4L)),
                    AttackType.NONE
            );
        }
    }

    /**
     * Two individually compliant requests occur inside the threshold window.
     * Their combined amount is 120% of the active automatic limit, causing the
     * second request to be blocked specifically by threshold-splitting logic.
     */
    private void generateThresholdAttack(
            List<SimulationRecord> records,
            String customerId,
            RefundPolicy policy,
            BigDecimal normalAmount) {

        BigDecimal attackAmount = policy.getAutomaticRefundLimit()
                .multiply(THRESHOLD_ATTACK_FACTOR)
                .setScale(2, RoundingMode.HALF_UP);

        for (int i = 0; i < REQUESTS_PER_CUSTOMER; i++) {
            if (i == 0) {
                addRequest(records, customerId, i,
                        "TX-" + customerId + "-" + i,
                        attackAmount, BASE_TIME, AttackType.NONE);
            } else if (i == 1) {
                addRequest(records, customerId, i,
                        "TX-" + customerId + "-" + i,
                        attackAmount, BASE_TIME.plus(Duration.ofHours(1)),
                        AttackType.THRESHOLD_SPLITTING);
            } else {
                Instant afterWindow = BASE_TIME
                        .plus(policy.getCumulativeExposureWindow())
                        .plus(Duration.ofDays(1L + (i - 2L) * 4L));
                addRequest(records, customerId, i,
                        "TX-" + customerId + "-" + i,
                        normalAmount, afterWindow, AttackType.NONE);
            }
        }
    }

    /**
     * Requests are spaced just beyond the threshold window, so velocity
     * detection cannot trigger. Their total crosses the cumulative limit only
     * on the final injected request.
     */
    private void generateCumulativeAttack(
            List<SimulationRecord> records,
            String customerId,
            RefundPolicy policy,
            BigDecimal normalAmount) {

        BigDecimal attackAmount = min(
                policy.getAutomaticRefundLimit().multiply(CUMULATIVE_ATTACK_AUTO_FACTOR),
                policy.getCustomerCumulativeLimit().multiply(CUMULATIVE_ATTACK_LIMIT_FACTOR)
        ).setScale(2, RoundingMode.HALF_UP);

        if (attackAmount.signum() <= 0
                || attackAmount.compareTo(policy.getAutomaticRefundLimit()) > 0) {
            throw new IllegalArgumentException(
                    "Could not construct a cumulative-exposure attack below the individual refund limit"
            );
        }

        int attackRequestCount = policy.getCustomerCumulativeLimit()
                .divideToIntegralValue(attackAmount)
                .intValueExact() + 1;

        if (attackRequestCount < 2 || attackRequestCount > REQUESTS_PER_CUSTOMER) {
            throw new IllegalArgumentException(
                    "Cumulative limit cannot be crossed using at most "
                            + REQUESTS_PER_CUSTOMER
                            + " individually compliant synthetic requests"
            );
        }

        Duration interval = policy.getThresholdSplittingWindow().plusSeconds(1);
        Duration attackSpan = interval.multipliedBy(attackRequestCount - 1L);
        if (attackSpan.compareTo(policy.getCumulativeExposureWindow()) > 0) {
            throw new IllegalArgumentException(
                    "Cumulative exposure window is too short to construct an isolated cumulative attack"
            );
        }

        for (int i = 0; i < REQUESTS_PER_CUSTOMER; i++) {
            if (i < attackRequestCount) {
                Instant timestamp = BASE_TIME.plus(interval.multipliedBy(i));
                AttackType label = i == attackRequestCount - 1
                        ? AttackType.CUMULATIVE_EXPOSURE
                        : AttackType.NONE;
                addRequest(records, customerId, i,
                        "TX-" + customerId + "-" + i,
                        attackAmount, timestamp, label);
            } else {
                Instant afterWindow = BASE_TIME
                        .plus(attackSpan)
                        .plus(policy.getCumulativeExposureWindow())
                        .plus(Duration.ofDays(1L + (i - attackRequestCount) * 4L));
                addRequest(records, customerId, i,
                        "TX-" + customerId + "-" + i,
                        normalAmount, afterWindow, AttackType.NONE);
            }
        }
    }

    private void generateDuplicateAttack(
            List<SimulationRecord> records,
            String customerId,
            BigDecimal normalAmount) {

        for (int i = 0; i < REQUESTS_PER_CUSTOMER; i++) {
            String transactionId;
            Instant timestamp;
            AttackType attack;

            if (i < 2) {
                transactionId = "PAYMENT-" + customerId;
                timestamp = BASE_TIME.plus(Duration.ofHours(i));
                attack = i == 1 ? AttackType.DUPLICATE_REQUEST : AttackType.NONE;
            } else {
                transactionId = "TX-" + customerId + "-" + i;
                timestamp = BASE_TIME.plus(Duration.ofDays(5L + (i - 2L) * 4L));
                attack = AttackType.NONE;
            }

            addRequest(records, customerId, i,
                    transactionId, normalAmount, timestamp, attack);
        }
    }

    private BigDecimal min(BigDecimal... values) {
        return Arrays.stream(values).min(BigDecimal::compareTo).orElseThrow();
    }

    private void addRequest(
            List<SimulationRecord> records,
            String customerId,
            int requestNumber,
            String transactionId,
            BigDecimal amount,
            Instant timestamp,
            AttackType attackType) {

        String requestId = String.format(
                "SIM-%s-%02d", customerId, requestNumber + 1
        );

        RefundRequest request = new RefundRequest(
                requestId,
                customerId,
                transactionId,
                amount,
                timestamp
        );

        records.add(new SimulationRecord(request, attackType));
    }

    private void printReport(SimulationReport report) {
        System.out.println("======================================");
        System.out.println("       POLICYSTRIKE SIMULATION");
        System.out.println("======================================");
        System.out.println("Customer Profiles: " + report.customerCount());
        System.out.println("Total Refund Requests: " + report.totalRequests());
        System.out.println("Auto Approved: " + report.approvedRequests());
        System.out.println("Manual Review: " + report.manualReviewRequests());
        System.out.println("Blocked: " + report.blockedRequests());
        System.out.println("Approved Financial Exposure: INR " + report.approvedExposure());
        System.out.println("Restricted Customers: " + report.restrictedCustomers());
        System.out.println("--------------------------------------");
        System.out.println("ATTACK DETECTION RESULTS");

        for (AttackType type : AttackType.values()) {
            if (type == AttackType.NONE) {
                continue;
            }
            int injected = report.injectedAttacks().getOrDefault(type, 0);
            int detected = report.detectedAttacks().getOrDefault(type, 0);
            System.out.println(type + ": " + detected + "/" + injected + " detected");
        }

        System.out.println("--------------------------------------");
        System.out.println("False Positive Blocks: " + report.falsePositiveBlocks());
        System.out.println("Missed Attacks: " + report.missedAttacks());
        System.out.println("======================================");
    }
}
