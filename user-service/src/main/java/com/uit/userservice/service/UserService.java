package com.uit.userservice.service;

import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import com.uit.userservice.dto.request.CreateUserRequest;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.entity.User;
import com.uit.userservice.entity.UserCredential;
import com.uit.userservice.entity.UserRole;
import com.uit.userservice.entity.UserRoleMapping;
import com.uit.userservice.mapper.UserMapper;
import com.uit.userservice.repository.UserCredentialRepository;
import com.uit.userservice.repository.UserRepository;
import com.uit.userservice.repository.UserRoleMappingRepository;
import com.uit.userservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final UserRoleMappingRepository mappingRepository;
    private final PasswordHasher passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest req, JwtAuthenticationToken token) {
        log.info("Creating user: {}", req.getUsername());

        if (userRepository.findByUsername(req.getUsername()).isPresent())
            throw new AppException(ErrorCode.USERNAME_EXISTS);

        if (userRepository.findByEmail(req.getEmail()).isPresent())
            throw new AppException(ErrorCode.EMAIL_EXISTS);

        // Tạo user
        User user = userRepository.save(User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .build());

        // Tạo credentials (hash password)
        String hashed = passwordEncoder.encode(req.getPassword());
        credentialRepository.save(UserCredential.builder()
                .user(user)
                .passwordHash(hashed)
                .build());

        // Ánh xạ roles
        List<String> requestedRoles = (req.getRoles() == null || req.getRoles().isEmpty())
                ? List.of("CUSTOMER")
                : req.getRoles();

        for (String roleName : requestedRoles) {
            UserRole role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_EXCEPTION));
            mappingRepository.save(new UserRoleMapping(user.getUserId(), role.getRoleId()));
        }

        // Trả về DTO
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .roles(requestedRoles)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(user -> {
                    // Lấy danh sách role name cho user này
                    List<UserRoleMapping> mappings = mappingRepository.findByUserId(user.getUserId());
                    List<String> roleNames = mappings.stream()
                            .map(mapping -> roleRepository.findById(mapping.getRoleId())
                                    .map(UserRole::getRoleName)
                                    .orElse(null))
                            .filter(Objects::nonNull)
                            .toList();

                    // Map sang DTO
                    UserResponse response = userMapper.toResponseDto(user);
                    response.setRoles(roleNames);
                    return response;
                })
                .toList();
    }

    public UserResponse getUserByToken(JwtAuthenticationToken token) {
        String userId = token.getToken().getSubject();
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_EXCEPTION));
        List<UserRoleMapping> mappings = mappingRepository.findByUserId(user.getUserId());
        List<String> roleNames = mappings.stream()
                .map(mapping -> roleRepository.findById(mapping.getRoleId())
                        .map(UserRole::getRoleName)
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        UserResponse response = userMapper.toResponseDto(user);
        response.setRoles(roleNames);
        return response;
    }
}
