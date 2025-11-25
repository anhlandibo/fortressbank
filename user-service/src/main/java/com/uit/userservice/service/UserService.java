package com.uit.userservice.service;

import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import com.uit.userservice.dto.request.CreateUserRequest;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.entity.User;
import com.uit.userservice.mapper.UserMapper;
import com.uit.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final Keycloak keycloak;

    @Transactional
    public UserResponse createUser(CreateUserRequest req, Map<String, Object> userInfo) {
        log.info("Creating user: {}", req.getUsername());

        // Create user in Keycloak
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(req.getUsername());
        userRepresentation.setEmail(req.getEmail());
        userRepresentation.setFirstName(req.getFirstName());
        userRepresentation.setLastName(req.getLastName());
        userRepresentation.setEnabled(true);

        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(req.getPassword());
        credentialRepresentation.setTemporary(false);
        userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));

        Response response = keycloak.realm("fortressbank-realm").users().create(userRepresentation);

        if (response.getStatus() == 409) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (response.getStatus() != 201) {
            throw new AppException(ErrorCode.USER_CREATION_FAILED);
        }

        // Create user in local database
        User user = userRepository.save(User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .build());

        return userMapper.toResponseDto(user);
    }

    public UserResponse getUserByToken(Map<String, Object> userInfo) {
        String userId = (String) userInfo.get("sub");
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_EXCEPTION));
        return userMapper.toResponseDto(user);
    }
}
