package com.uit.accountservice.client;

import com.uit.sharedkernel.api.ApiResponse;
import com.uit.accountservice.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// name: tên service trong Eureka/Docker compose, url: fallback nếu không dùng Eureka
@FeignClient(name = "user-service", url = "http://user-service:4000")
public interface UserClient {

    @GetMapping("/users/internal/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable("userId") String userId);
}