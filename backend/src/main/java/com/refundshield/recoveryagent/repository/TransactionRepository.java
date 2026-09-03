package com.refundshield.recoveryagent.repository;

import com.refundshield.recoveryagent.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCustomerId(Long customerId);
    List<Transaction> findByStatus(String status);
    List<Transaction> findByCustomerIdAndStatus(Long customerId, String status);
    long countByCustomerIdAndStatus(Long customerId, String status);
}
