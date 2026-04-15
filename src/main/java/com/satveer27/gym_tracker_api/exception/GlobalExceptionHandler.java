package com.satveer27.gym_tracker_api.exception;

import com.satveer27.gym_tracker_api.dto.errors.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("action=resource_not_found message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponseDto.of(404, "Not Found", ex.getMessage())
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("action=duplicate_resource message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponseDto.of(409, "Duplicate Resource", ex.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("action=type_mismatch message={}", ex.getMessage());
        String message = String.format("Invalid value for parameter '%s'", ex.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDto.of(HttpStatus.BAD_REQUEST.value(), "Type mismatch", message)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("action=message_not_readable message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDto.of(HttpStatus.BAD_REQUEST.value(), "Message not readable", "Request body is missing or malformed")
        );
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthorizedActionException(UnauthorizedActionException ex) {
        log.warn("action=unauthorized_action message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponseDto.of(HttpStatus.FORBIDDEN.value(), "Unauthorized", ex.getMessage())
        );
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handlePasswordMismatch(PasswordMismatchException ex) {
        log.warn("action=password_mismatch message={}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDto.of(HttpStatus.BAD_REQUEST.value(), "Password mismatch", ex.getMessage())
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach((fieldError)->{
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        });
        log.warn("action=validation_failed errors={}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDto.withFieldErrors(fieldErrors, "One or more fields is invalid", "Validation Failed")
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("action=no_resource_found message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponseDto.of(HttpStatus.NOT_FOUND.value(), "Not Found", "The requested endpoint does not exist")
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        log.warn("action=invalid_credentials message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponseDto.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex.getMessage())
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(AuthenticationException ex) {
        log.warn("action=authentication_exception message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponseDto.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Invalid username or password")
        );
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.warn("action=access_denied message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseDto.of(HttpStatus.FORBIDDEN.value(), "Forbidden", "You don't have permission to access this resource"));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingCookieException(MissingRequestCookieException ex) {
        log.warn("action=missing_cookie message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponseDto.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Missing cookie")
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponseDto> handleDisabledException(DisabledException ex) {
        log.warn("action=disabled_exception message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseDto.of(HttpStatus.FORBIDDEN.value(), "Forbidden", "Email not verified"));
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponseDto> handleMailException(MailException ex) {
        log.warn("action=mail_exception message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Error", "Failed to send email"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("action=method_not_supported message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponseDto.of(HttpStatus.METHOD_NOT_ALLOWED.value(), "Invalid method","Method not supported: " + ex.getMethod()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleInternalError(Exception ex) {
        log.error("action=unexpected_error message={}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponseDto.of(500,  "Internal Error", "An unexpected error occurred")
        );
    }
}
