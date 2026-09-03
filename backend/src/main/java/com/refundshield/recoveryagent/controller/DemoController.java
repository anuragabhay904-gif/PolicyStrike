package com.refundshield.recoveryagent.controller;

import com.refundshield.recoveryagent.dto.OpportunityDTO;
import com.refundshield.recoveryagent.dto.SimulateLeakRequest;
import com.refundshield.recoveryagent.service.DemoDataService;
import com.refundshield.recoveryagent.service.RazorpayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoDataService demoDataService;
    private final RazorpayService razorpayService;

    public DemoController(DemoDataService demoDataService, RazorpayService razorpayService) {
        this.demoDataService = demoDataService;
        this.razorpayService = razorpayService;
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedData() {
        demoDataService.seedDemoData();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Demo dataset successfully initialized with 10 customers and 26 transactions. Revenue leaks detected."
        ));
    }

    @PostMapping("/simulate-leak")
    public ResponseEntity<OpportunityDTO> simulateLeak(@RequestBody(required = false) SimulateLeakRequest request) {
        OpportunityDTO created = demoDataService.simulateLeak(request);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/simulate-failure")
    public ResponseEntity<Map<String, Object>> toggleFailure(@RequestParam(required = false) Boolean enabled) {
        boolean active = demoDataService.toggleFailureSimulation(enabled);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "failureSimulationActive", active,
                "message", active ? "Controlled failure scenario ENABLED. Subsequent Razorpay approvals will trigger simulated gateway failure."
                        : "Controlled failure scenario DISABLED. Razorpay approvals will operate normally."
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDemoStatus() {
        return ResponseEntity.ok(Map.of(
                "paymentMode", razorpayService.getPaymentMode(),
                "failureSimulationActive", razorpayService.isSimulateFailure()
        ));
    }
}
