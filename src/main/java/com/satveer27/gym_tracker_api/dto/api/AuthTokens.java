package com.satveer27.gym_tracker_api.dto.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthTokens {
    private String token;
    private String refreshToken;


    public static AuthTokens from(String token, String refreshToken) {
        return AuthTokens.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }
}
