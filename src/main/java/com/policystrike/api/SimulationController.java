
package com.policystrike.api;

import com.policystrike.api.dto.RefundAuditResponse;
import com.policystrike.api.dto.SimulationRunResponse;

import com.policystrike.persistence.entity.PolicyVersionEntity;
import com.policystrike.persistence.service.PolicyVersionService;
import com.policystrike.persistence.service.SimulationHistoryService;

import com.policystrike.simulation.PolicyStrikeSimulator;
import com.policystrike.simulation.SimulationExecution;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulations")
public class SimulationController {

    private final PolicyStrikeSimulator simulator;
    private final PolicyVersionService policyVersionService;
    private final SimulationHistoryService historyService;

    public SimulationController(
            PolicyStrikeSimulator simulator,
            PolicyVersionService policyVersionService,
            SimulationHistoryService historyService) {

        this.simulator = simulator;
        this.policyVersionService = policyVersionService;
        this.historyService = historyService;
    }

    @PostMapping("/run")
    public SimulationRunResponse runSimulation(
            @RequestParam(defaultValue = "PS-001") String policyId) {

        PolicyVersionEntity policyVersion =
                policyVersionService.getActiveVersion(policyId);

        SimulationExecution execution =
                simulator.runDetailed(policyVersion.toDomain());

        return historyService.saveRun(policyVersion, execution);
    }

    @GetMapping
    public List<SimulationRunResponse> getSimulationHistory() {
        return historyService.getHistory();
    }

    @GetMapping("/{runId}")
    public SimulationRunResponse getSimulation(
            @PathVariable UUID runId) {

        return historyService.getRun(runId);
    }

    @GetMapping("/{runId}/audits")
    public Page<RefundAuditResponse> getAuditRecords(
            @PathVariable UUID runId,
            @PageableDefault(
                    size = 50,
                    sort = "requestedAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        return historyService.getAuditRecords(runId, pageable);
    }
}
