package com.uit.accountservice.entity.enums;

public enum TransferStatus {
    PENDING,           // Awaiting OTP verification
    COMPLETED,         // Successfully completed
    FAILED,            // Failed (insufficient funds, etc.)
    REJECTED,          // Rejected by risk engine
    CANCELLED,         // Cancelled by user
    EXPIRED            // OTP/challenge expired
}
