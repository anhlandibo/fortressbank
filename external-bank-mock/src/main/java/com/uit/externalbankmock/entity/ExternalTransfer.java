package com.uit.externalbankmock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "external_transfers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalTransfer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String fortressBankTransactionId;

    @Column(nullable = false)
    private String sourceAccountNumber;

    @Column(nullable = false)
    private String sourceBankCode;

    @Column(nullable = false)
    private String destinationAccountNumber;

    @Column(nullable = false)
    private String destinationBankCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
