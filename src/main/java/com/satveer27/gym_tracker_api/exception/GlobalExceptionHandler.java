package com.satveer27.gym_tracker_api.exception;

import com.satveer27.gym_tracker_api.dto.errors.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDto.of(HttpStatus.BAD_REQUEST.value(), "Type mismatch", ex.getName())
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleInternalError(Exception ex) {
        log.error("action=unexpected_error message={}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponseDto.of(500,  "Internal Error", ex.getMessage())
        );
    }
}
