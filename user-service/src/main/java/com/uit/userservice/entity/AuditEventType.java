package com.uit.userservice.entity;

public enum AuditEventType {
    // Authentication events
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    PASSWORD_CHANGED,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,

    // Account management
    ACCOUNT_CREATED,
    ACCOUNT_UPDATED,
    ACCOUNT_CLOSED,

    // Role/Permission events
    ROLE_ASSIGNED,
    ROLE_REMOVED,

    // Critical operations
    TRANSACTION_INITIATED,
    TRANSACTION_APPROVED,
    TRANSACTION_REJECTED,

    // System events
    SYSTEM_ERROR
}
