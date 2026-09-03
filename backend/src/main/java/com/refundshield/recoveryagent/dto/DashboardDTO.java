package com.refundshield.recoveryagent.dto;

import com.refundshield.recoveryagent.entity.AuditEvent;
import java.math.BigDecimal;
import java.util.List;

public class DashboardDTO {
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal potentialRecoverableRevenue = BigDecimal.ZERO;
    private long activeOpportunitiesCount = 0;
    private BigDecimal recoveredRevenue = BigDecimal.ZERO;

    private BigDecimal successfulRevenue = BigDecimal.ZERO;
    private BigDecimal failedRevenue = BigDecimal.ZERO;
    private BigDecimal abandonedRevenue = BigDecimal.ZERO;

    private long totalCustomers = 0;
    private long totalTransactions = 0;
    private String paymentMode; // RAZORPAY or DEMO
    private boolean failureSimulationActive;

    private List<OpportunityDTO> recentOpportunities;
    private List<AuditEvent> recentActivities;

    public DashboardDTO() {}

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getPotentialRecoverableRevenue() { return potentialRecoverableRevenue; }
    public void setPotentialRecoverableRevenue(BigDecimal potentialRecoverableRevenue) { this.potentialRecoverableRevenue = potentialRecoverableRevenue; }

    public long getActiveOpportunitiesCount() { return activeOpportunitiesCount; }
    public void setActiveOpportunitiesCount(long activeOpportunitiesCount) { this.activeOpportunitiesCount = activeOpportunitiesCount; }

    public BigDecimal getRecoveredRevenue() { return recoveredRevenue; }
    public void setRecoveredRevenue(BigDecimal recoveredRevenue) { this.recoveredRevenue = recoveredRevenue; }

    public BigDecimal getSuccessfulRevenue() { return successfulRevenue; }
    public void setSuccessfulRevenue(BigDecimal successfulRevenue) { this.successfulRevenue = successfulRevenue; }

    public BigDecimal getFailedRevenue() { return failedRevenue; }
    public void setFailedRevenue(BigDecimal failedRevenue) { this.failedRevenue = failedRevenue; }

    public BigDecimal getAbandonedRevenue() { return abandonedRevenue; }
    public void setAbandonedRevenue(BigDecimal abandonedRevenue) { this.abandonedRevenue = abandonedRevenue; }

    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }

    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public boolean isFailureSimulationActive() { return failureSimulationActive; }
    public void setFailureSimulationActive(boolean failureSimulationActive) { this.failureSimulationActive = failureSimulationActive; }

    public List<OpportunityDTO> getRecentOpportunities() { return recentOpportunities; }
    public void setRecentOpportunities(List<OpportunityDTO> recentOpportunities) { this.recentOpportunities = recentOpportunities; }

    public List<AuditEvent> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<AuditEvent> recentActivities) { this.recentActivities = recentActivities; }
}
