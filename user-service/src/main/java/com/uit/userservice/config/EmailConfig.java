package com.uit.userservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fortress-bank.email")
@Data
public class EmailConfig {
    private String from;
    private String fromName;
    private OtpConfig otp;

    @Data
    public static class OtpConfig {
        private String subject;
        private Integer expiryMinutes;
    }
}
