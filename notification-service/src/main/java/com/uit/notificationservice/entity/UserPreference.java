package com.uit.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    @Id
    @Column(columnDefinition = "CHAR(36)", name = "user_id")
    private String userId;

    @Column(nullable = false, name = "device_token")
    private List<String> deviceTokens;

    @Column(nullable = false, name = "push_enabled")
    private boolean pushEnabled;

    @Column(nullable = false, name = "email_enabled")
    private boolean emailEnabled;

    @Column(name = "sms_enabled")
    private boolean smsEnabled;
}
