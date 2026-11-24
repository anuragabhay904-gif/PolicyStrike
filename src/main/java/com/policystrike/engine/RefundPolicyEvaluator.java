package com.policystrike.engine;

import com.policystrike.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class RefundPolicyEvaluator {

    private final Map<String, ProcessedRequest> processedRequests =
            new HashMap<>();

    private final Set<CustomerTransaction> seenTransactions =
            new HashSet<>();

    private final Map<String, List<RefundRequest>> approvedRefunds =
            new HashMap<>();

    public synchronized RefundDecision evaluate(
            RefundPolicy policy,
            RefundRequest request) {

        Objects.requireNonNull(policy, "Policy cannot be null");
        Objects.requireNonNull(request, "Request cannot be null");

        ProcessedRequest previous =
                processedRequests.get(request.getRequestId());

        if (previous != null) {
            if (samePayload(previous.request(), request)) {
                return previous.decision();
            }

            return new RefundDecision(
                    request.getRequestId(),
                    DecisionStatus.BLOCKED,
                    "Request ID reused with different refund details"
            );
        }

        CustomerTransaction transactionKey =
                new CustomerTransaction(
                        request.getCustomerId(),
                        request.getTransactionId()
                );

        // Duplicate detection is controlled by the policy version.
        if (policy.isDuplicateProtectionEnabled()
                && seenTransactions.contains(transactionKey)) {

            return record(request, new RefundDecision(
                    request.getRequestId(),
                    DecisionStatus.BLOCKED,
                    "Duplicate refund request for the same transaction"
            ));
        }

        if (policy.isDuplicateProtectionEnabled()) {
            seenTransactions.add(transactionKey);
        }

        RefundDecision decision;

        // Individual refund threshold.
        if (request.getAmount().compareTo(
                policy.getAutomaticRefundLimit()) > 0) {

            decision = new RefundDecision(
                    request.getRequestId(),
                    DecisionStatus.MANUAL_REVIEW,
                    "Individual refund exceeds automatic refund limit"
            );

        } else {

            // Threshold splitting detection.
            BigDecimal recentExposure = calculateExposure(
                    request.getCustomerId(),
                    request.getRequestedAt(),
                    policy.getThresholdSplittingWindow()
            );

            BigDecimal splitTotal =
                    recentExposure.add(request.getAmount());

            if (splitTotal.compareTo(
                    policy.getAutomaticRefundLimit()) > 0) {

                decision = new RefundDecision(
                        request.getRequestId(),
                        DecisionStatus.BLOCKED,
                        "Threshold splitting detected within monitoring window"
                );

            } else {

                // Cumulative exposure detection.
                BigDecimal cumulativeExposure = calculateExposure(
                        request.getCustomerId(),
                        request.getRequestedAt(),
                        policy.getCumulativeExposureWindow()
                );

                BigDecimal projectedExposure =
                        cumulativeExposure.add(request.getAmount());

                if (projectedExposure.compareTo(
                        policy.getCustomerCumulativeLimit()) > 0) {

                    decision = new RefundDecision(
                            request.getRequestId(),
                            DecisionStatus.BLOCKED,
                            "Customer cumulative refund exposure limit exceeded"
                    );

                } else {

                    decision = new RefundDecision(
                            request.getRequestId(),
                            DecisionStatus.AUTO_APPROVE,
                            "Refund passed all deterministic policy checks"
                    );

                    approvedRefunds
                            .computeIfAbsent(
                                    request.getCustomerId(),
                                    key -> new ArrayList<>())
                            .add(request);
                }
            }
        }

        return record(request, decision);
    }

    private BigDecimal calculateExposure(
            String customerId,
            Instant currentTime,
            java.time.Duration window) {

        Instant windowStart = currentTime.minus(window);

        List<RefundRequest> customerRefunds =
                approvedRefunds.getOrDefault(customerId, List.of());

        BigDecimal total = BigDecimal.ZERO;

        for (RefundRequest refund : customerRefunds) {

            Instant refundTime = refund.getRequestedAt();

            boolean insideWindow =
                    !refundTime.isBefore(windowStart)
                    && !refundTime.isAfter(currentTime);

            if (insideWindow) {
                total = total.add(refund.getAmount());
            }
        }

        return total;
    }

    private RefundDecision record(
            RefundRequest request,
            RefundDecision decision) {

        processedRequests.put(
                request.getRequestId(),
                new ProcessedRequest(request, decision)
        );

        return decision;
    }

    private boolean samePayload(
            RefundRequest first,
            RefundRequest second) {

        return first.getCustomerId().equals(second.getCustomerId())
                && first.getTransactionId().equals(second.getTransactionId())
                && first.getAmount().compareTo(second.getAmount()) == 0
                && first.getRequestedAt().equals(second.getRequestedAt());
    }

    private record CustomerTransaction(
            String customerId,
            String transactionId) {}

    private record ProcessedRequest(
            RefundRequest request,
            RefundDecision decision) {}
}
