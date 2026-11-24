package com.policystrike.engine;

import com.policystrike.domain.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RefundPolicyEvaluatorTest {

    private final RefundPolicyEvaluator evaluator =
            new RefundPolicyEvaluator();

    private final RefundPolicy policy = new RefundPolicy(
            "PS-001",
            1,
            new BigDecimal("7500"),
            new BigDecimal("20000"),
            Duration.ofHours(24),
            Duration.ofDays(30)
    );

    private static final Instant BASE_TIME =
            Instant.parse("2026-01-01T00:00:00Z");

    private RefundRequest request(
            String requestId,
            String customerId,
            String transactionId,
            String amount,
            Instant timestamp) {

        return new RefundRequest(
                requestId,
                customerId,
                transactionId,
                new BigDecimal(amount),
                timestamp
        );
    }

    @Test
    void shouldApproveRefundBelowLimit() {
        RefundDecision decision = evaluator.evaluate(
                policy,
                request("R1", "C1", "T1", "5000", BASE_TIME)
        );

        assertEquals(DecisionStatus.AUTO_APPROVE, decision.getStatus());
    }

    @Test
    void shouldApproveRefundExactlyAtLimit() {
        RefundDecision decision = evaluator.evaluate(
                policy,
                request("R2", "C2", "T2", "7500", BASE_TIME)
        );

        assertEquals(DecisionStatus.AUTO_APPROVE, decision.getStatus());
    }

    @Test
    void shouldSendIndividualRefundAboveLimitForManualReview() {
        RefundDecision decision = evaluator.evaluate(
                policy,
                request("R3", "C3", "T3", "8000", BASE_TIME)
        );

        assertEquals(DecisionStatus.MANUAL_REVIEW, decision.getStatus());
    }

    @Test
    void shouldDetectThresholdSplitting() {
        RefundDecision first = evaluator.evaluate(
                policy,
                request("R4", "C4", "T4", "4000", BASE_TIME)
        );

        RefundDecision second = evaluator.evaluate(
                policy,
                request("R5", "C4", "T5", "4000",
                        BASE_TIME.plus(Duration.ofHours(1)))
        );

        assertEquals(DecisionStatus.AUTO_APPROVE, first.getStatus());
        assertEquals(DecisionStatus.BLOCKED, second.getStatus());
        assertTrue(second.getReason().contains("Threshold splitting"));
    }

    @Test
    void shouldDetectCumulativeCustomerExposure() {
        evaluator.evaluate(
                policy,
                request("R6", "C5", "T6", "6000", BASE_TIME)
        );

        evaluator.evaluate(
                policy,
                request("R7", "C5", "T7", "6000",
                        BASE_TIME.plus(Duration.ofDays(2)))
        );

        evaluator.evaluate(
                policy,
                request("R8", "C5", "T8", "6000",
                        BASE_TIME.plus(Duration.ofDays(4)))
        );

        RefundDecision fourth = evaluator.evaluate(
                policy,
                request("R9", "C5", "T9", "6000",
                        BASE_TIME.plus(Duration.ofDays(6)))
        );

        assertEquals(DecisionStatus.BLOCKED, fourth.getStatus());
        assertTrue(fourth.getReason().contains("cumulative"));
    }

    @Test
    void shouldDetectDuplicateTransactionRefund() {
        RefundDecision first = evaluator.evaluate(
                policy,
                request("R10", "C6", "PAYMENT-100", "1000", BASE_TIME)
        );

        RefundDecision second = evaluator.evaluate(
                policy,
                request("R11", "C6", "PAYMENT-100", "1000",
                        BASE_TIME.plus(Duration.ofHours(1)))
        );

        assertEquals(DecisionStatus.AUTO_APPROVE, first.getStatus());
        assertEquals(DecisionStatus.BLOCKED, second.getStatus());
        assertTrue(second.getReason().contains("Duplicate"));
    }

    @Test
    void shouldReturnOriginalDecisionForExactRetry() {
        RefundRequest original = request(
                "R12", "C7", "T12", "1000", BASE_TIME);

        RefundDecision first = evaluator.evaluate(policy, original);
        RefundDecision retry = evaluator.evaluate(policy, original);

        assertSame(first, retry);
    }

    @Test
    void shouldBlockRequestIdReusedWithDifferentPayload() {
        evaluator.evaluate(
                policy,
                request("R13", "C8", "T13", "1000", BASE_TIME)
        );

        RefundDecision changedRequest = evaluator.evaluate(
                policy,
                request("R13", "C8", "T13", "2000", BASE_TIME)
        );

        assertEquals(DecisionStatus.BLOCKED, changedRequest.getStatus());
    }

    @Test
    void shouldNotCountRefundsOutsideCumulativeWindow() {
        RefundDecision first = evaluator.evaluate(
                policy,
                request("R14", "C9", "T14", "6000", BASE_TIME)
        );

        RefundDecision second = evaluator.evaluate(
                policy,
                request("R15", "C9", "T15", "6000",
                        BASE_TIME.plus(Duration.ofDays(31)))
        );

        assertEquals(DecisionStatus.AUTO_APPROVE, first.getStatus());
        assertEquals(DecisionStatus.AUTO_APPROVE, second.getStatus());
    }
}
