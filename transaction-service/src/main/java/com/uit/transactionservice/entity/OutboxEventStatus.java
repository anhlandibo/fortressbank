package com.uit.transactionservice.entity;

public enum OutboxEventStatus {
    PENDING,       // Chưa xử lý - mới tạo
    PROCESSING,    // Đang publish
    COMPLETED,     // Đã publish thành công
    FAILED         // Publish thất bại - cần retry
}
