package com.uit.transactionservice.exception;

/**
 * Exception thrown when account service communication fails
 */
public class AccountServiceException extends RuntimeException {
    
    public AccountServiceException(String message) {
        super(message);
    }
    
    public AccountServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
