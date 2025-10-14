package com.uit.userservice.mapper;

import com.uit.userservice.dto.request.CreateUserRequest;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(CreateUserRequest dto);

    UserResponse toResponseDto(User entity);
}
