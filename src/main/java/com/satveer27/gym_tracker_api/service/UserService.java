package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.dto.users.*;
import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.enums.Role;
import com.satveer27.gym_tracker_api.enums.TokenType;
import com.satveer27.gym_tracker_api.exception.*;
import com.satveer27.gym_tracker_api.repository.RefreshTokenRepository;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import com.satveer27.gym_tracker_api.repository.VerificationTokenRepository;
import com.satveer27.gym_tracker_api.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationService emailVerificationService;
    private final VerificationTokenRepository verificationTokenRepository;

    public UserResponse getUserById(Long id){
        log.debug("action=get_user_by_id id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.debug("action=get_user user_id={} status=found", id);
        return UserResponse.from(user);
    }

    public UserResponse updateUserByIdAdmin(Long id, UpdateUserRequestAdmin request){
        log.debug("action=update_user_by_id id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.debug("action=update_user user_id={} status=found", id);
        if(request.getUsername() != null){
            if(userRepository.existsByUsernameIgnoreCase(request.getUsername()) && !user.getUsername().equals(request.getUsername())){
                throw new DuplicateResourceException("Username already exists");
            }
            user.setUsername(request.getUsername().toLowerCase());
        }

        boolean emailChanged = false;

        if(request.getEmail() != null){
            if(userRepository.existsByEmailIgnoreCase(request.getEmail())  && !user.getEmail().equals(request.getEmail().toLowerCase())){
                throw new DuplicateResourceException("Email already exists");
            }
            if(!user.getEmail().equals(request.getEmail().toLowerCase())){
                user.setEmail(request.getEmail().toLowerCase());
                user.setEmailVerified(false);
                emailChanged =  true;
            }
        }

        if(request.getRole() != null){
            user.setRole(request.getRole());
        }

        userRepository.save(user);
        if(emailChanged){
            refreshTokenRepository.deleteByUserId(user.getId());
            emailVerificationService.sendEmail(request.getEmail().toLowerCase(), TokenType.EMAIL_VERIFICATION);
        }
        log.info("action=update_user_admin user_id={} username={} status=success", id,  user.getUsername());
        return UserResponse.from(user);
    }

    public UserResponse updateUserById(Long id, UpdatedUserRequest request){
        log.debug("action=update_user_by_username userId={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.debug("action=update_user_by_username user_id={} status=found", user.getId());
        if(request.getUsername() != null){
            if(userRepository.existsByUsernameIgnoreCase(request.getUsername()) && !user.getUsername().equals(request.getUsername())){
                throw new DuplicateResourceException("Username already exists");
            }
            user.setUsername(request.getUsername().toLowerCase());
        }

        if(request.getRole() != null){
            if(request.getRole() == Role.ADMIN){
                throw new UnauthorizedActionException("You are not allowed to perform this action");
            }
            user.setRole(request.getRole());
        }

        userRepository.save(user);
        log.info("action=update_user user_id={} username={} status=success", user.getId(),  user.getUsername());
        return UserResponse.from(user);
    }

    public void deleteUserById(Long id){
        log.debug("action=delete_user_by_id id={}", id);
        if(userRepository.existsById(id)){
            userRepository.deleteById(id);
            log.info("action=delete_user_user_id={} status=success", id);
        }else{
            throw  new ResourceNotFoundException("User not found");
        }
    }

    public GetAllUsersResponse getAllUser(Pageable pageable, String role, String username, String email,
                                          LocalDateTime start, LocalDateTime end){
        log.debug("action=get_all_users");
        Specification<User> spec = Specification
                .where(UserSpecification.hasRole(role))
                .and(UserSpecification.hasUserName(username))
                .and(UserSpecification.hasEmail(email))
                .and(UserSpecification.betweenTimeStamp(start, end));
        Page<User> result = userRepository.findAll(spec, pageable);
        GetAllUsersResponse response = GetAllUsersResponse.from(result);
        return response;
    }

    public void updateUserPassword(Long id, UpdatePasswordRequest request){
        log.debug("action=update_user_password_by_id id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if(!request.getNewPassword().equals(request.getConfirmNewPassword())){
            throw new PasswordMismatchException("Passwords don't match");
        }

        if(!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())){
            throw new InvalidCredentialsException("Old passwords don't match");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(user.getId());
    }


}
