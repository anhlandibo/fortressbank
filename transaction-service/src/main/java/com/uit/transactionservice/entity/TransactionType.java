package com.uit.transactionservice.entity;

public enum TransactionType {
    INTERNAL_TRANSFER,  // Chuyển nội bộ
    EXTERNAL_TRANSFER,  // Chuyển ngoại bộ
    BILL_PAYMENT,       // Thanh toán hóa đơn
    DEPOSIT,            // Nạp tiền
    WITHDRAWAL          // Rút tiền
}
