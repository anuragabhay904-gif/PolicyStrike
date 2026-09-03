package com.refundshield.recoveryagent.controller;

import com.refundshield.recoveryagent.entity.Transaction;
import com.refundshield.recoveryagent.repository.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<Transaction> getTransactions(@RequestParam(required = false) Long customerId,
                                             @RequestParam(required = false) String status) {
        if (customerId != null && status != null) {
            return transactionRepository.findByCustomerIdAndStatus(customerId, status.toUpperCase());
        } else if (customerId != null) {
            return transactionRepository.findByCustomerId(customerId);
        } else if (status != null) {
            return transactionRepository.findByStatus(status.toUpperCase());
        }
        return transactionRepository.findAll();
    }
}
