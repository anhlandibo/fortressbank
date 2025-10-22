package com.uit.userservice.repository;

import com.uit.userservice.entity.UserRoleMapping;
import com.uit.userservice.entity.UserRoleMappingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleMappingRepository extends JpaRepository<UserRoleMapping, UserRoleMappingId> {
    List<UserRoleMapping> findByUserId(String userId);
}
