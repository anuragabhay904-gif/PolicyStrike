package com.refundshield.recoveryagent.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Value("${razorpay.mode:DEMO}")
    private String paymentMode;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Revenue Leak Recovery Agent",
                "timestamp", LocalDateTime.now().toString(),
                "paymentMode", paymentMode,
                "aiModel", geminiModel
        ));
    }
}
