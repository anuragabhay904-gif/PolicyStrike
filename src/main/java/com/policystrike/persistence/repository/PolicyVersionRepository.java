package com.policystrike.persistence.repository;

import com.policystrike.persistence.entity.PolicyVersionEntity;
import com.policystrike.persistence.entity.PolicyVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyVersionRepository extends JpaRepository<PolicyVersionEntity, Long> {

    Optional<PolicyVersionEntity> findFirstByPolicyIdOrderByVersionDesc(String policyId);

    Optional<PolicyVersionEntity> findFirstByPolicyIdAndStatusOrderByVersionDesc(
            String policyId, PolicyVersionStatus status);

    Optional<PolicyVersionEntity> findByPolicyIdAndVersion(String policyId, int version);

    List<PolicyVersionEntity> findAllByPolicyIdOrderByVersionDesc(String policyId);

    List<PolicyVersionEntity> findAllByPolicyIdAndStatusOrderByVersionDesc(
            String policyId, PolicyVersionStatus status);

    boolean existsByPolicyId(String policyId);

    boolean existsByPolicyIdAndStatus(String policyId, PolicyVersionStatus status);
}
