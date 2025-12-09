package com.uit.accountservice.entity;

import com.uit.accountservice.entity.enums.AccountStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @UuidGenerator
    @Column(name = "account_id")
    private String accountId;

    @NotNull
    @Size(min = 10, max = 20)
    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "pin_hash")
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
