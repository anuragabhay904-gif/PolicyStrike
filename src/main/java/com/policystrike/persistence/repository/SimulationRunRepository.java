
package com.policystrike.persistence.repository;

import com.policystrike.persistence.entity.SimulationRunEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationRunRepository
        extends JpaRepository<SimulationRunEntity, UUID> {

    List<SimulationRunEntity> findAllByOrderByCreatedAtDesc();
}
