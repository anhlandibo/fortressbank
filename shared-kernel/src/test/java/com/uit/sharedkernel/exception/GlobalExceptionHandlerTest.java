package com.uit.sharedkernel.exception;

import com.uit.sharedkernel.api.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private WebRequest webRequest;

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Test
    @DisplayName("handleAppException() returns correct error response")
    void testHandleAppException() {
        AppException ex = new AppException(ErrorCode.NOT_FOUND_EXCEPTION);
        when(webRequest.getDescription(false)).thenReturn("uri=/test");

        ResponseEntity<ApiResponse<ErrorDetail>> response = exceptionHandler.handleAppException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(404);
        assertThat(response.getBody().getData().getException()).isEqualTo("NOT_FOUND_EXCEPTION");
    }

    @Test
    @DisplayName("handleException() returns 500 error response")
    void testHandleException() {
        Exception ex = new RuntimeException("Unexpected error");
        when(webRequest.getDescription(false)).thenReturn("uri=/test");

        ResponseEntity<ApiResponse<ErrorDetail>> response = exceptionHandler.handleException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(500);
        assertThat(response.getBody().getData().getException()).isEqualTo("RuntimeException");
    }

    @Test
    @DisplayName("handleValidationException() returns 400 error response")
    void testHandleValidationException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "must not be null");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(400);
        assertThat(response.getBody().getData()).containsEntry("field", "must not be null");
    }
}

