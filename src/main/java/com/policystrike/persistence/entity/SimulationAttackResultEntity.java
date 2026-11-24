
package com.policystrike.persistence.entity;

import com.policystrike.simulation.AttackType;

import jakarta.persistence.*;

@Entity
@Table(name = "simulation_attack_results")
public class SimulationAttackResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_run_id", nullable = false)
    private SimulationRunEntity simulationRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "attack_type", nullable = false, length = 50)
    private AttackType attackType;

    @Column(name = "injected_count", nullable = false)
    private int injectedCount;

    @Column(name = "detected_count", nullable = false)
    private int detectedCount;

    protected SimulationAttackResultEntity() {
    }

    public SimulationAttackResultEntity(
            SimulationRunEntity simulationRun,
            AttackType attackType,
            int injectedCount,
            int detectedCount) {

        this.simulationRun = simulationRun;
        this.attackType = attackType;
        this.injectedCount = injectedCount;
        this.detectedCount = detectedCount;
    }

    public AttackType getAttackType() {
        return attackType;
    }

    public int getInjectedCount() {
        return injectedCount;
    }

    public int getDetectedCount() {
        return detectedCount;
    }
}
