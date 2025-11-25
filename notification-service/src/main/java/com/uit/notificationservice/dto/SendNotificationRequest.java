package com.uit.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    private String notificationId;
    private String userId;
    private String title;
    private String content;
    private String image;
    private String type;
    private boolean isRead;
    private Date sentAt;
    private String deviceToken;
}
