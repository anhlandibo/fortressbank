package com.uit.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter @NoArgsConstructor
@AllArgsConstructor @Builder
public class CreateRoleRequest {
    @NotBlank(message = "Role name is required")
    private String roleName;
}
