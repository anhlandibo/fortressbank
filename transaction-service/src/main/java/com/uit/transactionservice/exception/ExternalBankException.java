package com.uit.transactionservice.exception;

public class ExternalBankException extends RuntimeException {
    public ExternalBankException(String message) {
        super(message);
    }

    public ExternalBankException(String message, Throwable cause) {
        super(message, cause);
    }
}
