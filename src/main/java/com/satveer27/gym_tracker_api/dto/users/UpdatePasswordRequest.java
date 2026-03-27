package com.satveer27.gym_tracker_api.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePasswordRequest {

    @NotEmpty(message="Old password is required")
    @Size(min=8, message = "Password must be at least 8 characters")
    private String oldPassword;

    @NotEmpty(message="New Password is required")
    @Size(min=8, message = "Password must be at least 8 characters")
    private String newPassword;

    @NotEmpty(message="Confirm new password is required")
    @Size(min=8, message = "Password must be at least 8 characters")
    private String confirmNewPassword;
}
