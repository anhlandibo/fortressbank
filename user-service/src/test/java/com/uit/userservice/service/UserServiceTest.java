package com.uit.userservice.service;

import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import com.uit.userservice.dto.request.CreateUserRequest;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.entity.User;
import com.uit.userservice.mapper.UserMapper;
import com.uit.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Keycloak keycloak;
    
    @Mock
    private RealmResource realmResource;
    
    @Mock
    private UsersResource usersResource;
    
    @Mock
    private Response response;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        // Lenient stubbing for Keycloak chain to avoid unnecessary stubbing exceptions
        // in tests that don't use create user flow
        lenient().when(keycloak.realm("fortressbank-realm")).thenReturn(realmResource);
        lenient().when(realmResource.users()).thenReturn(usersResource);
    }

    @Test
    @DisplayName("createUser() creates user successfully")
    void testCreateUser_Success() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setPassword("password");
        
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
                
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("testuser");
        when(userMapper.toResponseDto(user)).thenReturn(userResponse);
        
        UserResponse result = userService.createUser(request, Collections.emptyMap());
        
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(usersResource).create(any(UserRepresentation.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser() throws exception when Keycloak fails")
    void testCreateUser_ThrowsException_WhenKeycloakFails() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(409); // Conflict
        
        assertThatThrownBy(() -> userService.createUser(request, Collections.emptyMap()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.USER_CREATION_FAILED));
                
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUserByToken() returns user")
    void testGetUserByToken_Success() {
        Map<String, Object> userInfo = Map.of("sub", "user-123");
        User user = User.builder().username("testuser").build();
        
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername("testuser");
        when(userMapper.toResponseDto(user)).thenReturn(userResponse);
        
        UserResponse result = userService.getUserByToken(userInfo);
        
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUserByToken() throws exception when user not found")
    void testGetUserByToken_ThrowsNotFound() {
        Map<String, Object> userInfo = Map.of("sub", "user-123");
        
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userService.getUserByToken(userInfo))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_EXCEPTION));
    }
}

