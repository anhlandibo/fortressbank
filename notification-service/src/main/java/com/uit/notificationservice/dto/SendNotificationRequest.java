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
    private String userId;
    private String title;
    private String content;
    private String image;
    private String type;
    private boolean isRead;
    private Date sentAt;
    private String deviceToken;

    public SendNotificationRequest(String userId, String title, String content, String image, String type, boolean isRead, Date sentAt) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.image = image;
        this.type = type;
        this.isRead = isRead;
        this.sentAt = sentAt;
    }
}
