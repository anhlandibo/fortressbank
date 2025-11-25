package com.uit.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    @Id
    @Column(columnDefinition = "CHAR(36)", name = "user_id")
    private String userId;

//    @C

    @Column(nullable = false, name = "email_enabled")
    private boolean emailEnabled;

    @Column(name = "sms_enabled")
    private boolean smsEnabled;
}
