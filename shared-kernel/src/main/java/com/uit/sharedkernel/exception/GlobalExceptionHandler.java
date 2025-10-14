package com.uit.sharedkernel.exception;

import com.uit.sharedkernel.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<ErrorDetail>> handleAppException(AppException ex, WebRequest request) {
        ErrorDetail errorDetail = new ErrorDetail(
                request.getDescription(false),
                ex.getErrorCode().name(),
                ex.getErrorCode().getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(ex.getErrorCode().getStatusCode())
                .body(ApiResponse.error(
                        ex.getErrorCode().getStatusCode().value(),
                        ex.getErrorCode().getMessage(),
                        errorDetail
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorDetail>> handleException(Exception ex, WebRequest request) {
        ErrorDetail detail = new ErrorDetail(
                request.getDescription(false),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "Internal Server Error", detail));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(400, "Validation failed", errors));
    }
}
