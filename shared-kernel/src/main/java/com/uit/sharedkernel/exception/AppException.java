package com.uit.sharedkernel.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {
    private ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // ADD THIS CONSTRUCTOR
    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage); // Use the custom message from AccountService
        this.errorCode = errorCode;
    }
}