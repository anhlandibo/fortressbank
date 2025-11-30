package com.uit.userservice.service;

import com.uit.userservice.dto.request.UpdateUserRequest;
import com.uit.userservice.dto.response.UserResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserService {

    UserResponse getCurrentUser(Jwt jwt);

    UserResponse updateCurrentUser(Jwt jwt, UpdateUserRequest request);
}
