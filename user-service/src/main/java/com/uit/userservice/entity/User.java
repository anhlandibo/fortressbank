package com.uit.userservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    private String id;        // = Keycloak userId (sub)
    private String username;  // preferred_username
    private String email;
    private String fullName;
    private LocalDateTime createdAt = LocalDateTime.now();
}
