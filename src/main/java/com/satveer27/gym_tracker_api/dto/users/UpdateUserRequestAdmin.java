package com.satveer27.gym_tracker_api.dto.users;

import com.satveer27.gym_tracker_api.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequestAdmin {
    @Size(min = 3, max=50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Invalid email format")
    private String email;

    private Role role;

    public void setUsername(String username){
        if(username == null || username.isBlank()){
            this.username = null;
        }else{
            this.username = username.toLowerCase();
        }
    }

    public void setEmail(String email){
        if(email == null || email.isBlank()){
            this.email = null;
        }else{
            this.email = email.toLowerCase();
        }
    }
}
