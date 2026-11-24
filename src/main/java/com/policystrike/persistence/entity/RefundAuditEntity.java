
package com.policystrike.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.policystrike.domain.DecisionStatus;
import com.policystrike.simulation.AttackType;
import com.policystrike.simulation.EvaluationRecord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "refund_audit_records",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_run_request",
            columnNames = {"simulation_run_id", "request_id"}
        )
    },
    indexes = {
        @Index(
            name = "idx_audit_run_requested_at",
            columnList = "simulation_run_id, requested_at"
        ),
        @Index(
            name = "idx_audit_customer",
            columnList = "customer_id"
        )
    }
)
public class RefundAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_run_id", nullable = false)
    private SimulationRunEntity simulationRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_version_id", nullable = false)
    private PolicyVersionEntity policyVersion;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "transaction_id", nullable = false, length = 150)
    private String transactionId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_status", nullable = false, length = 30)
    private DecisionStatus decisionStatus;

    @Column(name = "decision_reason", nullable = false, columnDefinition = "text")
    private String decisionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_label", nullable = false, length = 50)
    private AttackType scenarioLabel;

    protected RefundAuditEntity() {
    }

    public RefundAuditEntity(
            SimulationRunEntity simulationRun,
            PolicyVersionEntity policyVersion,
            EvaluationRecord evaluation) {

        this.simulationRun = simulationRun;
        this.policyVersion = policyVersion;

        this.requestId = evaluation.request().getRequestId();
        this.customerId = evaluation.request().getCustomerId();
        this.transactionId = evaluation.request().getTransactionId();
        this.amount = evaluation.request().getAmount();
        this.requestedAt = evaluation.request().getRequestedAt();

        this.evaluatedAt = Instant.now();

        this.decisionStatus = evaluation.decision().getStatus();
        this.decisionReason = evaluation.decision().getReason();

        this.scenarioLabel = evaluation.scenarioLabel();
    }

    public UUID getId() {
        return id;
    }

    public SimulationRunEntity getSimulationRun() {
        return simulationRun;
    }

    public PolicyVersionEntity getPolicyVersion() {
        return policyVersion;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public DecisionStatus getDecisionStatus() {
        return decisionStatus;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public AttackType getScenarioLabel() {
        return scenarioLabel;
    }
}
