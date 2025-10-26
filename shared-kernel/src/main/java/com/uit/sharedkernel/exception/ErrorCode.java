package com.uit.sharedkernel.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR), NOT_FOUND_EXCEPTION(404, "Card not found", HttpStatus.NOT_FOUND), BLOCK_CARD_CONFLICT(409, "Card is already blocked", HttpStatus.CONFLICT), ACTIVATE_CARD_CONFLICT(409, "Card is already activated", HttpStatus.CONFLICT), USERNAME_EXISTS(409, "Username already exists", HttpStatus.CONFLICT), EMAIL_EXISTS(409, "Email already exists", HttpStatus.CONFLICT), ROLE_EXISTS(409, "Role already exists", HttpStatus.CONFLICT),

    /* Account */
    ACCOUNT_NOT_FOUND(404, "Account not found", HttpStatus.NOT_FOUND), ACCOUNT_STATUS_CONFLICT(409, "Invalid account status transition", HttpStatus.CONFLICT), ACCOUNT_CLOSE_NONZERO_BALANCE(409, "Cannot close account with non-zero balance", HttpStatus.CONFLICT), ACCOUNT_INVALID_BALANCE(400, "Invalid balance amount", HttpStatus.BAD_REQUEST), INSUFFICIENT_FUNDS(400, "Insufficient funds", HttpStatus.BAD_REQUEST), FORBIDDEN(403, "You do not have permission to perform this action", HttpStatus.FORBIDDEN), INVALID_OTP(400, "Invalid OTP code", HttpStatus.BAD_REQUEST), RISK_ASSESSMENT_FAILED(500, "Risk assessment failed", HttpStatus.INTERNAL_SERVER_ERROR), NOTIFICATION_SERVICE_FAILED(500, "Notification service failed", HttpStatus.INTERNAL_SERVER_ERROR), REDIS_CONNECTION_FAILED(500, "Redis connection failed", HttpStatus.INTERNAL_SERVER_ERROR),

    /* User */
    USER_CREATION_FAILED(500, "User creation failed", HttpStatus.INTERNAL_SERVER_ERROR);

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}

