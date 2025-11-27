package com.uit.transactionservice.repository;

import com.uit.transactionservice.entity.TransactionOTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionOTPRepository extends JpaRepository<TransactionOTP, UUID> {
    
    Optional<TransactionOTP> findByTransactionIdAndVerifiedFalse(UUID transactionId);
    
    Optional<TransactionOTP> findByTransactionIdAndOtpCodeAndVerifiedFalse(UUID transactionId, String otpCode);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
