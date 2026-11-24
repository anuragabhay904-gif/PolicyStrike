package com.policystrike.simulation;

import com.policystrike.domain.RefundRequest;

import java.util.Objects;

public record SimulationRecord(
        RefundRequest request,
        AttackType expectedAttack) {

    public SimulationRecord {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(expectedAttack, "Attack type is required");
    }
}
