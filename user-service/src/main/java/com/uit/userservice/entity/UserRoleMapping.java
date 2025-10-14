package com.uit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_role_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserRoleMappingId.class)
public class UserRoleMapping {
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Id
    @Column(name = "role_id")
    private Integer roleId;
}