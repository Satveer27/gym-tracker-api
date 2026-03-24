package com.satveer27.gym_tracker_api.dto.users;

import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private LocalDate createdAt;
    private Role role;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(LocalDate.from(user.getCreatedAt()))
                .role(user.getRole())
                .build();
    }
}
