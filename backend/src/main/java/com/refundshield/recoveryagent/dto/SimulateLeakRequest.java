package com.refundshield.recoveryagent.dto;

import java.math.BigDecimal;

public class SimulateLeakRequest {
    private String type; // FAILED_PAYMENT_RECOVERY, HIGH_VALUE_CHECKOUT_RECOVERY, REPEAT_CUSTOMER_REENGAGEMENT
    private BigDecimal amount;
    private Long customerId;

    public SimulateLeakRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
}
