package com.uit.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLimit {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "daily_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "daily_used", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyUsed;

    @Column(name = "last_daily_reset")
    private LocalDateTime lastDailyReset;

    @Column(name = "monthly_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "monthly_used", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyUsed;

    @Column(name = "last_monthly_reset")
    private LocalDateTime lastMonthlyReset;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
