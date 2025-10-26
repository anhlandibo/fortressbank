package com.uit.userservice.entity;

public enum UserStatus {
    PENDING_VERIFICATION,  // Initial state when user is created
    ACTIVE,               // User is verified and active
    LOCKED,               // Account is locked (e.g., too many failed attempts)
    DISABLED              // Account is disabled/closed
}
