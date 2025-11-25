package com.uit.referenceservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankResponse {
    private String bankCode;
    private String bankName;
    private String logoUrl;
    private String status;
    private LocalDateTime createdAt;
}

