
package com.policystrike.persistence.service;

import com.policystrike.api.dto.RefundAuditResponse;
import com.policystrike.api.dto.SimulationRunResponse;

import com.policystrike.persistence.entity.PolicyVersionEntity;
import com.policystrike.persistence.entity.RefundAuditEntity;
import com.policystrike.persistence.entity.SimulationRunEntity;

import com.policystrike.persistence.repository.RefundAuditRepository;
import com.policystrike.persistence.repository.SimulationRunRepository;

import com.policystrike.simulation.SimulationExecution;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SimulationHistoryService {

    private final SimulationRunRepository runRepository;
    private final RefundAuditRepository auditRepository;

    public SimulationHistoryService(
            SimulationRunRepository runRepository,
            RefundAuditRepository auditRepository) {

        this.runRepository = runRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public SimulationRunResponse saveRun(
            PolicyVersionEntity policyVersion,
            SimulationExecution execution) {

        // 1. Save and flush the parent simulation record.
        SimulationRunEntity runEntity =
                new SimulationRunEntity(
                        policyVersion,
                        execution.report()
                );

        SimulationRunEntity savedRun =
                runRepository.saveAndFlush(runEntity);

        // 2. Convert every evaluation into a database audit record.
        List<RefundAuditEntity> auditRecords =
                execution.evaluations()
                        .stream()
                        .map(evaluation ->
                                new RefundAuditEntity(
                                        savedRun,
                                        policyVersion,
                                        evaluation
                                ))
                        .toList();

        // 3. Persist all individual decisions.
        auditRepository.saveAll(auditRecords);

        // The transaction commits after this method completes.
        return SimulationRunResponse.from(savedRun);
    }

    @Transactional(readOnly = true)
    public List<SimulationRunResponse> getHistory() {

        return runRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(SimulationRunResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SimulationRunResponse getRun(UUID runId) {

        SimulationRunEntity entity =
                runRepository.findById(runId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Simulation run not found"
                                ));

        return SimulationRunResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public Page<RefundAuditResponse> getAuditRecords(
            UUID runId,
            Pageable pageable) {

        if (!runRepository.existsById(runId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Simulation run not found"
            );
        }

        return auditRepository
                .findAllBySimulationRun_IdOrderByRequestedAtAsc(
                        runId, pageable
                )
                .map(RefundAuditResponse::from);
    }
}
