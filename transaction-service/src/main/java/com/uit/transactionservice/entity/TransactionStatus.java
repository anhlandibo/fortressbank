package com.uit.transactionservice.entity;

public enum TransactionStatus {
    PENDING_OTP,   // Đang chờ xác thực OTP
    PENDING,       // Đang chờ xử lý (sau khi OTP verified)
    PROCESSING,    // Đang xử lý
    COMPLETED,     // Hoàn thành
    FAILED,        // Thất bại
    CANCELLED,     // Đã hủy
    OTP_EXPIRED    // OTP hết hạn
}
