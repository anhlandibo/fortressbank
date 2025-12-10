package com.uit.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {
    @Id
    @Column(columnDefinition = "CHAR(36)", name = "user_id")
    private String userId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_device_tokens",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "device_token")
    private List<String> deviceTokens = new ArrayList<>();

    @Column(nullable = false, name = "push_notification_enabled")
    private boolean pushNotificationEnabled;

    @Column(nullable = false, name = "sms_notification_enabled")
    private boolean smsNotificationEnabled;

    @Column(nullable = false, name = "email_notification_enabled")
    private boolean emailNotificationEnabled;
}
