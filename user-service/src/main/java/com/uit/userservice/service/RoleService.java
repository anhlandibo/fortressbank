package com.uit.userservice.service;

import com.uit.userservice.dto.request.CreateRoleRequest;
import com.uit.userservice.dto.response.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    @Transactional
    public RoleResponse createRole(CreateRoleRequest req) {
        return null;
    }
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return Collections.emptyList();
    }
}
