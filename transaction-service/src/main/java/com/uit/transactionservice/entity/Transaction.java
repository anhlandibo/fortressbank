package com.uit.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_sender_date", columnList = "sender_account_id,created_at"),
    @Index(name = "idx_receiver_date", columnList = "receiver_account_id,created_at"),
    @Index(name = "idx_status_date", columnList = "status,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "tx_id")
    private java.util.UUID txId;

    @Column(name = "sender_account_id", nullable = false)
    private String senderAccountId;

    @Column(name = "receiver_account_id", nullable = false)
    private String receiverAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_amount", precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "tx_type", length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionType txType;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
