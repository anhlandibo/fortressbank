package com.uit.userservice.service;

import com.uit.userservice.dto.request.CreateRoleRequest;
import com.uit.userservice.dto.response.RoleResponse;
import com.uit.userservice.entity.UserRole;
import com.uit.userservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final UserRoleRepository roleRepository;

    @Transactional
    public RoleResponse createRole(CreateRoleRequest req) {
        if (roleRepository.findByRoleName(req.getRoleName()).isPresent()) {
            throw new RuntimeException("Role already exists");
        }
        UserRole role = UserRole.builder()
                .roleName(req.getRoleName().toUpperCase())
                .build();
        UserRole saved = roleRepository.save(role);
        return new RoleResponse(saved.getRoleId(), saved.getRoleName());
    }
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> new RoleResponse(role.getRoleId(), role.getRoleName()))
                .collect(Collectors.toList());
    }
}

