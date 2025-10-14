package com.uit.userservice.dto.response;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class RoleResponse {
    private Integer roleId;
    private String roleName;
}
