package com.uit.notificationservice.mapper;

import com.uit.notificationservice.dto.SendNotificationRequest;
import com.uit.notificationservice.dto.SendNotificationResponse;
import com.uit.notificationservice.entity.NotificationMessage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMessageMapper {

    // Entity -> Response DTO
    SendNotificationResponse toResponseDto(NotificationMessage message);

    // Request DTO -> Entity
    NotificationMessage toEntity(SendNotificationRequest dto);

    List<SendNotificationResponse> toResponseDto(List<NotificationMessage> notificationList);
}
