package com.uit.sharedkernel.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse Unit Tests")
class ApiResponseTest {

    @Test
    @DisplayName("success() creates success response")
    void testSuccess() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(data);

        assertThat(response.getCode()).isEqualTo(1000);
        assertThat(response.getMessage()).isEqualTo("Success");
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("error() creates error response")
    void testError() {
        String data = "error detail";
        ApiResponse<String> response = ApiResponse.error(404, "Not Found", data);

        assertThat(response.getCode()).isEqualTo(404);
        assertThat(response.getMessage()).isEqualTo("Not Found");
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("Setters and getters work correctly")
    void testSettersAndGetters() {
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(202);
        response.setMessage("Accepted");
        response.setData("processing");

        assertThat(response.getCode()).isEqualTo(202);
        assertThat(response.getMessage()).isEqualTo("Accepted");
        assertThat(response.getData()).isEqualTo("processing");
    }

    @Test
    @DisplayName("AllArgsConstructor works correctly")
    void testAllArgsConstructor() {
        ApiResponse<String> response = new ApiResponse<>(500, "Internal Error", "details");

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("Internal Error");
        assertThat(response.getData()).isEqualTo("details");
    }
}

