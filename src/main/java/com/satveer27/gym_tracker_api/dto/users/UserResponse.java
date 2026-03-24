package com.satveer27.gym_tracker_api.dto.users;

import com.satveer27.gym_tracker_api.enums.Role;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private LocalDate createdAt;
    private Role role;
}
