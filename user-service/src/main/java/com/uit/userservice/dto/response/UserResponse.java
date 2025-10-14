package com.uit.userservice.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Integer userId;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    private List<String> roles;
}
