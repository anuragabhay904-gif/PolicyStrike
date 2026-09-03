package com.refundshield.recoveryagent.service;

import com.refundshield.recoveryagent.entity.Customer;
import com.refundshield.recoveryagent.entity.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class RecoveryScoreCalculator {

    @Value("${app.leak-detection.high-value-threshold:500.00}")
    private BigDecimal highValueThreshold;

    public int calculateScore(String leakType, Customer customer, Transaction transaction, long customerFailedCount) {
        int score = 0;

        switch (leakType) {
            case "FAILED_PAYMENT_RECOVERY" -> {
                // +30 failed transaction
                score += 30;

                // +20 amount > configured threshold
                if (transaction != null && transaction.getAmount().compareTo(highValueThreshold) >= 0) {
                    score += 20;
                }

                // +20 customer is repeat customer
                if (customer != null && (customer.getTotalOrders() > 1 || customer.getTotalSpent().compareTo(new BigDecimal("1000")) >= 0)) {
                    score += 20;
                }

                // +15 multiple failed attempts
                if (customerFailedCount > 1) {
                    score += 15;
                }

                // +15 recent transaction (within last 7 days)
                if (transaction != null && transaction.getTransactionDate() != null) {
                    long daysAgo = ChronoUnit.DAYS.between(transaction.getTransactionDate(), LocalDateTime.now());
                    if (daysAgo <= 7) {
                        score += 15;
                    }
                }
            }
            case "HIGH_VALUE_CHECKOUT_RECOVERY" -> {
                // Base 35
                score += 35;

                if (transaction != null) {
                    if (transaction.getAmount().compareTo(new BigDecimal("5000")) >= 0) {
                        score += 35;
                    } else if (transaction.getAmount().compareTo(new BigDecimal("2000")) >= 0) {
                        score += 25;
                    } else if (transaction.getAmount().compareTo(highValueThreshold) >= 0) {
                        score += 15;
                    }

                    if (transaction.getTransactionDate() != null) {
                        long daysAgo = ChronoUnit.DAYS.between(transaction.getTransactionDate(), LocalDateTime.now());
                        if (daysAgo <= 3) {
                            score += 15;
                        } else if (daysAgo <= 7) {
                            score += 10;
                        }
                    }
                }

                if (customer != null && customer.getTotalOrders() > 0) {
                    score += 15;
                }
            }
            case "REPEAT_CUSTOMER_REENGAGEMENT" -> {
                // Base 40
                score += 40;

                if (customer != null) {
                    if (customer.getTotalSpent().compareTo(new BigDecimal("5000")) >= 0) {
                        score += 25;
                    } else if (customer.getTotalSpent().compareTo(new BigDecimal("2000")) >= 0) {
                        score += 15;
                    }

                    if (customer.getTotalOrders() >= 3) {
                        score += 20;
                    } else if (customer.getTotalOrders() >= 1) {
                        score += 10;
                    }

                    if (customer.getLastOrderDate() != null) {
                        long daysInactive = ChronoUnit.DAYS.between(customer.getLastOrderDate(), LocalDateTime.now());
                        if (daysInactive <= 60) {
                            score += 15;
                        } else if (daysInactive <= 90) {
                            score += 10;
                        }
                    }
                }
            }
            default -> score = 50;
        }

        // Clamp between 0 and 100
        return Math.max(0, Math.min(100, score));
    }
}
