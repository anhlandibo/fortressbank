package com.uit.userservice.entity;

public enum SecurityEventSeverity {
    CRITICAL,   // Immediate action required, potential breach
    HIGH,       // Urgent attention needed, significant risk
    MEDIUM,     // Notable security concern
    LOW,        // Minor security event
    INFO        // Informational security event
}
