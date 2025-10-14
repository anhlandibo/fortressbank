package com.uit.sharedkernel.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_FOUND_EXCEPTION(404, "Card not found", HttpStatus.NOT_FOUND),
    BLOCK_CARD_CONFLICT(409, "Card is already blocked", HttpStatus.CONFLICT),
    ACTIVATE_CARD_CONFLICT(409, "Card is already activated", HttpStatus.CONFLICT),
    USERNAME_EXISTS(409, "Username already exists", HttpStatus.CONFLICT),
    EMAIL_EXISTS(409, "Email already exists", HttpStatus.CONFLICT),
    ROLE_EXISTS(409, "Role already exists", HttpStatus.CONFLICT),

    /* Account */
    ACCOUNT_NOT_FOUND(404, "Account not found", HttpStatus.NOT_FOUND),
    ACCOUNT_STATUS_CONFLICT(409, "Invalid account status transition", HttpStatus.CONFLICT),
    ACCOUNT_CLOSE_NONZERO_BALANCE(409, "Cannot close account with non-zero balance", HttpStatus.CONFLICT),
    ACCOUNT_INVALID_BALANCE(400, "Invalid balance amount", HttpStatus.BAD_REQUEST);
    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
    private int code;
    private String message;
    private HttpStatusCode statusCode;
}

