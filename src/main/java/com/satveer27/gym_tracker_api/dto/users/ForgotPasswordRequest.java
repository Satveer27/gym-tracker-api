package com.satveer27.gym_tracker_api.dto.users;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {
    @NotEmpty(message="New Password is required")
    @Size(min=8, message = "Password must be at least 8 characters")
    private String newPassword;

    @NotEmpty(message="Confirm new password is required")
    @Size(min=8, message = "Password must be at least 8 characters")
    private String confirmNewPassword;
}
