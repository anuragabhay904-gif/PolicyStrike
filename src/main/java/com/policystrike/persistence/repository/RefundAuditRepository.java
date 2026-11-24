
package com.policystrike.persistence.repository;

import com.policystrike.persistence.entity.RefundAuditEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefundAuditRepository
        extends JpaRepository<RefundAuditEntity, UUID> {

    Page<RefundAuditEntity>
    findAllBySimulationRun_IdOrderByRequestedAtAsc(
            UUID simulationRunId,
            Pageable pageable
    );

    long countBySimulationRun_Id(UUID simulationRunId);
}
