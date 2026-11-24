
package com.policystrike.persistence.entity;

import com.policystrike.simulation.AttackType;
import com.policystrike.simulation.SimulationReport;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "simulation_runs")
public class SimulationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_version_id", nullable = false)
    private PolicyVersionEntity policyVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    private int customerCount;
    private int totalRequests;
    private int approvedRequests;
    private int manualReviewRequests;
    private int blockedRequests;

    @Column(precision = 19, scale = 2)
    private BigDecimal approvedExposure;

    private int restrictedCustomers;
    private int falsePositiveBlocks;
    private int missedAttacks;

    @OneToMany(
        mappedBy = "simulationRun",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<SimulationAttackResultEntity> attackResults =
            new ArrayList<>();

    protected SimulationRunEntity() {
    }

    public SimulationRunEntity(
            PolicyVersionEntity policyVersion,
            SimulationReport report) {

        this.policyVersion = policyVersion;
        this.createdAt = Instant.now();

        this.customerCount = report.customerCount();
        this.totalRequests = report.totalRequests();
        this.approvedRequests = report.approvedRequests();
        this.manualReviewRequests = report.manualReviewRequests();
        this.blockedRequests = report.blockedRequests();
        this.approvedExposure = report.approvedExposure();
        this.restrictedCustomers = report.restrictedCustomers();
        this.falsePositiveBlocks = report.falsePositiveBlocks();
        this.missedAttacks = report.missedAttacks();

        for (AttackType type : AttackType.values()) {
            if (type == AttackType.NONE) {
                continue;
            }

            int injected = report.injectedAttacks()
                    .getOrDefault(type, 0);

            int detected = report.detectedAttacks()
                    .getOrDefault(type, 0);

            attackResults.add(
                    new SimulationAttackResultEntity(
                            this, type, injected, detected
                    )
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public PolicyVersionEntity getPolicyVersion() {
        return policyVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getCustomerCount() {
        return customerCount;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getApprovedRequests() {
        return approvedRequests;
    }

    public int getManualReviewRequests() {
        return manualReviewRequests;
    }

    public int getBlockedRequests() {
        return blockedRequests;
    }

    public BigDecimal getApprovedExposure() {
        return approvedExposure;
    }

    public int getRestrictedCustomers() {
        return restrictedCustomers;
    }

    public int getFalsePositiveBlocks() {
        return falsePositiveBlocks;
    }

    public int getMissedAttacks() {
        return missedAttacks;
    }

    public List<SimulationAttackResultEntity> getAttackResults() {
        return attackResults;
    }
}
