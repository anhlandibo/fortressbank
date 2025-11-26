package com.uit.userservice.mapper;

import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper Unit Tests")
class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("toResponseDto() maps User to UserResponse")
    void testToResponseDto() {
        User user = User.builder()
                .userId("u1")
                .username("john")
                .email("john@example.com")
                .build();

        UserResponse dto = mapper.toResponseDto(user);

        assertThat(dto).isNotNull();
        assertThat(dto.getUserId()).isEqualTo(user.getUserId());
        assertThat(dto.getUsername()).isEqualTo(user.getUsername());
        assertThat(dto.getEmail()).isEqualTo(user.getEmail());
    }
}

