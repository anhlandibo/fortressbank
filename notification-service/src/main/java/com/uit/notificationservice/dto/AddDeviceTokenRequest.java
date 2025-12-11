package com.uit.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddDeviceTokenRequest {
    
    @NotBlank(message = "Device token is required")
    private String deviceToken;
}
