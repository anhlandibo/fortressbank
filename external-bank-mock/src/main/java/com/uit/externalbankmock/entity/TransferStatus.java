package com.uit.externalbankmock.entity;

public enum TransferStatus {
    PENDING,      // Just received request
    PROCESSING,   // Being processed by external bank
    COMPLETED,    // Successfully completed
    FAILED        // Failed (insufficient balance, invalid account, etc.)
}
