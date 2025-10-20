package com.uit.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TextBeeRequest {
    private String[] recipients;
    private String message;
}
