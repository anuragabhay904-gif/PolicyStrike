
package com.policystrike.simulation;

import java.util.List;
import java.util.Objects;

public record SimulationExecution(
        SimulationReport report,
        List<EvaluationRecord> evaluations) {

    public SimulationExecution {
        Objects.requireNonNull(report, "Simulation report is required");
        evaluations = List.copyOf(evaluations);
    }
}
