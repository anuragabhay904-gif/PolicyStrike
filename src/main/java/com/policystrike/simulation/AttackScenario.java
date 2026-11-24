package com.policystrike.simulation;

import com.policystrike.domain.RefundRequest;

import java.util.List;
import java.util.Objects;

public record AttackScenario(
        AttackType attackType,
        List<RefundRequest> requests) {

    public AttackScenario {
        Objects.requireNonNull(attackType);
        Objects.requireNonNull(requests);

        if (attackType == AttackType.NONE || requests.isEmpty()) {
            throw new IllegalArgumentException(
                    "A replay scenario needs an attack and refund requests");
        }

        requests = List.copyOf(requests);
    }
}
