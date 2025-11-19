package com.uit.transactionservice.repository;

import com.uit.transactionservice.entity.Transaction;
import com.uit.transactionservice.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, java.util.UUID> {

    Page<Transaction> findBySenderAccountId(String senderAccountId, Pageable pageable);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    Page<Transaction> findBySenderAccountIdOrReceiverAccountId(String senderAccountId, String receiverAccountId, Pageable pageable);

    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Transaction t WHERE t.senderAccountId = :accountId OR t.receiverAccountId = :accountId")
    Page<Transaction> findByAccountId(String accountId, Pageable pageable);

    @Query("SELECT SUM(t.amount + t.feeAmount) FROM Transaction t " +
           "WHERE t.senderAccountId = :accountId " +
           "AND DATE(t.createdAt) = CURRENT_DATE " +
           "AND t.status IN ('COMPLETED', 'PROCESSING')")
    BigDecimal calculateDailyUsed(String accountId);

    @Query("SELECT SUM(t.amount + t.feeAmount) FROM Transaction t " +
           "WHERE t.senderAccountId = :accountId " +
           "AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) " +
           "AND MONTH(t.createdAt) = MONTH(CURRENT_DATE) " +
           "AND t.status IN ('COMPLETED', 'PROCESSING')")
    BigDecimal calculateMonthlyUsed(String accountId);
}
