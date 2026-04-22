package io.github.mystagogy.insuranceinterface.common.exception;

import io.github.mystagogy.insuranceinterface.common.response.ApiResponse;
import io.github.mystagogy.insuranceinterface.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다.";
        ErrorResponse error = new ErrorResponse(Instant.now(), request.getRequestURI(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
        AuthenticationException exception,
        HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(Instant.now(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
        AccessDeniedException exception,
        HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(Instant.now(), request.getRequestURI(), "접근 권한이 없습니다.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(Instant.now(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception path={} message={}", request.getRequestURI(), exception.getMessage(), exception);
        ErrorResponse error = new ErrorResponse(Instant.now(), request.getRequestURI(), "서버 내부 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(error));
    }
}
