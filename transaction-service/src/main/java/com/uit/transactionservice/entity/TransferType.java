package com.uit.transactionservice.entity;

public enum TransferType {
    INTERNAL_TRANSFER,    // Transfer within same bank (FortressBank)
    EXTERNAL_TRANSFER         // Transfer to external bank (interbank)
}
