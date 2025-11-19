package com.uit.transactionservice.entity;

public enum TransactionStatus {
    PENDING,       // Đang chờ xử lý
    PROCESSING,    // Đang xử lý
    COMPLETED,     // Hoàn thành
    FAILED,        // Thất bại
    CANCELLED      // Đã hủy
}
