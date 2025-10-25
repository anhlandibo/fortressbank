package com.uit.userservice.controller;

import com.uit.sharedkernel.api.ApiResponse;
import com.uit.userservice.dto.request.CreateUserRequest;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.service.UserService;
import com.uit.userservice.security.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) httpRequest.getAttribute("userInfo");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.createUser(request, userInfo)));
    }

    @GetMapping("/me")
    @RequireRole("user") // Allow both admin and user to get their own details
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(HttpServletRequest httpRequest) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) httpRequest.getAttribute("userInfo");
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByToken(userInfo)));
    }
}
