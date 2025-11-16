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
public class BranchResponse {
    private Integer branchId;
    private String bankCode;
    private String branchName;
    private String address;
    private String city;
    private String status;
    private LocalDateTime createdAt;
}

