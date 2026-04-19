package com.satveer27.gym_tracker_api.dto.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApiResponse {
    private String message;

    public static ApiResponse from(String message) {
        return ApiResponse.builder().message(message).build();
    }
}
