package com.uit.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationRequest {
    private String recipientEmail;
    private String recipientName;
    private String title;
    private String content;
    
    // Optional: Badge (SUCCESS, FAILED, INFO, WARNING)
    private String badge;
    
    // Optional: Additional info table (key-value pairs)
    private List<InfoRow> additionalInfo;
    
    // Optional: Call to action
    private String ctaUrl;
    private String ctaText;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InfoRow {
        private String label;
        private String value;
    }
}
