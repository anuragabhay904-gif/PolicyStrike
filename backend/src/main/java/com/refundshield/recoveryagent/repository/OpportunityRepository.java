package com.refundshield.recoveryagent.repository;

import com.refundshield.recoveryagent.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {
    List<Opportunity> findByStatus(String status);
    List<Opportunity> findByCustomerId(Long customerId);
    Optional<Opportunity> findByTransactionId(Long transactionId);
    boolean existsByTransactionId(Long transactionId);
    boolean existsByCustomerIdAndTypeAndStatusNot(Long customerId, String type, String status);
    List<Opportunity> findAllByOrderByCreatedAtDesc();
}
