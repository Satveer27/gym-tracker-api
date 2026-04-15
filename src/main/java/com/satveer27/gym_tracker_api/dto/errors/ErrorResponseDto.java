package com.satveer27.gym_tracker_api.dto.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
public class ErrorResponseDto {
    private String message;
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String requestId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> fieldErrors;

    public static ErrorResponseDto of(int status, String error, String message) {
        return ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .requestId(MDC.get("requestId"))
                .build();
    }

    public static ErrorResponseDto withFieldErrors(Map<String, String> fieldErrors, String message, String error) {
        return ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error(error)
                .message(message)
                .fieldErrors(fieldErrors)
                .requestId(MDC.get("requestId"))
                .build();
    }
}
