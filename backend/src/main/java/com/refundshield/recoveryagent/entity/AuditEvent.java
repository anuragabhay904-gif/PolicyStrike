package com.refundshield.recoveryagent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long opportunityId;

    @Column(nullable = false)
    private String action; // OPPORTUNITY_DETECTED, AI_ANALYSIS_GENERATED, OPPORTUNITY_REVIEWED, RECOVERY_APPROVED, PAYMENT_LINK_CREATED, PAYMENT_LINK_FAILED, RECOVERY_DISMISSED, DEMO_SEEDED

    @Column(nullable = false)
    private String actor; // SYSTEM, MERCHANT, AI_AGENT

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public AuditEvent() {}

    public AuditEvent(Long opportunityId, String action, String actor, String details) {
        this.opportunityId = opportunityId;
        this.action = action;
        this.actor = actor;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOpportunityId() { return opportunityId; }
    public void setOpportunityId(Long opportunityId) { this.opportunityId = opportunityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
