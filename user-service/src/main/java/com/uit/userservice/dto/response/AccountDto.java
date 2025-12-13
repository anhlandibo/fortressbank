package com.uit.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    private String accountId;
    private String userId;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private String accountNumber;
    private String accountStatus;
}
