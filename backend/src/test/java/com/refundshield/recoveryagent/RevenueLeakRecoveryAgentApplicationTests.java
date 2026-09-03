package com.refundshield.recoveryagent;

import com.refundshield.recoveryagent.dto.ApproveRecoveryResponse;
import com.refundshield.recoveryagent.dto.OpportunityDTO;
import com.refundshield.recoveryagent.entity.Opportunity;
import com.refundshield.recoveryagent.exception.PaymentGatewayException;
import com.refundshield.recoveryagent.repository.OpportunityRepository;
import com.refundshield.recoveryagent.service.DemoDataService;
import com.refundshield.recoveryagent.service.OpportunityService;
import com.refundshield.recoveryagent.service.RevenueLeakDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RevenueLeakRecoveryAgentApplicationTests {

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private OpportunityService opportunityService;

    @Autowired
    private com.refundshield.recoveryagent.service.RazorpayService razorpayService;

    @BeforeEach
    void setUp() {
        razorpayService.setSimulateFailure(false);
        demoDataService.seedDemoData();
    }

    @Test
    void testDemoDataSeedingAndLeakDetection() {
        List<Opportunity> opportunities = opportunityRepository.findAll();
        assertFalse(opportunities.isEmpty(), "Opportunities should be detected after seeding demo data");

        // Verify each leak category exists
        boolean hasFailedPayment = opportunities.stream().anyMatch(o -> "FAILED_PAYMENT_RECOVERY".equals(o.getType()));
        boolean hasAbandoned = opportunities.stream().anyMatch(o -> "HIGH_VALUE_CHECKOUT_RECOVERY".equals(o.getType()));
        boolean hasRepeatCustomer = opportunities.stream().anyMatch(o -> "REPEAT_CUSTOMER_REENGAGEMENT".equals(o.getType()));

        assertTrue(hasFailedPayment, "Should detect failed payment recovery opportunities");
        assertTrue(hasAbandoned, "Should detect high value checkout recovery opportunities");
        assertTrue(hasRepeatCustomer, "Should detect repeat customer re-engagement opportunities");

        // Verify deterministic scoring (0-100)
        for (Opportunity opp : opportunities) {
            assertTrue(opp.getRecoveryScore() >= 0 && opp.getRecoveryScore() <= 100,
                    "Recovery score must be clamped between 0 and 100");
        }
    }

    @Test
    void testAiAnalysisFallback() {
        Opportunity opp = opportunityRepository.findAll().get(0);
        OpportunityDTO analyzed = opportunityService.analyzeOpportunity(opp.getId());

        assertNotNull(analyzed.getAiExplanation());
        assertNotNull(analyzed.getAiRecommendation());
        assertNotNull(analyzed.getSuggestedMessage());
        assertNotNull(analyzed.getConfidence());
        assertEquals("REVIEWED", analyzed.getStatus());
    }

    @Test
    void testApproveOpportunityFlow() {
        Opportunity opp = opportunityRepository.findAll().stream()
                .filter(o -> "NEW".equals(o.getStatus()))
                .findFirst()
                .orElseThrow();

        ApproveRecoveryResponse response = opportunityService.approveOpportunity(opp.getId());

        assertTrue(response.isSuccess());
        assertNotNull(response.getPaymentLink());
        assertTrue(response.getPaymentLink().contains("rzp.io/i/demo_"));
        assertEquals("RECOVERY_CREATED", response.getStatus());

        OpportunityDTO updated = opportunityService.getOpportunity(opp.getId());
        assertEquals("RECOVERY_CREATED", updated.getStatus());
        assertEquals(response.getPaymentLink(), updated.getPaymentLink());
    }

    @Test
    void testSimulatedFailureScenario() {
        // Enable failure simulation
        demoDataService.toggleFailureSimulation(true);

        Opportunity opp = opportunityRepository.findAll().stream()
                .filter(o -> "NEW".equals(o.getStatus()) || "REVIEWED".equals(o.getStatus()))
                .findFirst()
                .orElseThrow();

        assertThrows(PaymentGatewayException.class, () -> {
            opportunityService.approveOpportunity(opp.getId());
        });

        OpportunityDTO failedOpp = opportunityService.getOpportunity(opp.getId());
        assertEquals("FAILED", failedOpp.getStatus());

        // Restore failure simulation to false
        demoDataService.toggleFailureSimulation(false);
    }
}
