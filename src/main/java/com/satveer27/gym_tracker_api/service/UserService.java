package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.dto.users.*;
import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.enums.Role;
import com.satveer27.gym_tracker_api.exception.*;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import com.satveer27.gym_tracker_api.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserResponse register(UserRegisterRequest request) {
        log.debug("action=register_user username={}", request.getUsername());

        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail().toLowerCase());
        String encodedPassword = bCryptPasswordEncoder.encode(request.getPassword());
        user.setPasswordHash(encodedPassword);
        User saved = userRepository.save(user);
        log.info("action=register_user user_id={} username={} status=success",
                saved.getId(), saved.getUsername());
        return UserResponse.from(saved);
    }

    public UserResponse getUserById(Long id){
        log.debug("action=get_user_by_id id={}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            log.debug("action=get_user user_id={} status=found", id);
            return UserResponse.from(user.get());
        }else{
            throw new ResourceNotFoundException("User not found");
        }
    }

    public UserResponse updateUserById(Long id, UpdatedUserRequest request){
        log.debug("action=update_user_by_id id={}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            log.debug("action=update_user user_id={} status=found", id);
            if(request.getUsername() != null){
                if(userRepository.existsByUsernameIgnoreCase(request.getUsername()) && !user.get().getUsername().equals(request.getUsername())){
                    throw new DuplicateResourceException("Username already exists");
                }
                user.get().setUsername(request.getUsername());
            }

            if(request.getEmail() != null){
                if(userRepository.existsByEmailIgnoreCase(request.getEmail())  && !user.get().getEmail().equals(request.getEmail().toLowerCase())){
                    throw new DuplicateResourceException("Email already exists");
                }
                user.get().setEmail(request.getEmail());
            }

            if(request.getRole() != null){
                if(request.getRole() == Role.ADMIN){
                    throw new UnauthorizedActionException("unauthorized action");
                }
                user.get().setRole(request.getRole());
            }

            userRepository.save(user.get());
            log.info("action=update_user user_id={} username={} status=success", id,  user.get().getUsername());
            return UserResponse.from(user.get());

        }else{
            throw new ResourceNotFoundException("User not found");
        }
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
        log.debug("action=update_user_by_id id={}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            if(!request.getNewPassword().equals(request.getConfirmNewPassword())){
                throw new PasswordMismatchException("Passwords don't match");
            }

            if(!bCryptPasswordEncoder.matches(request.getOldPassword(), user.get().getPasswordHash())){
                throw new InvalidCredentialsException("Old passwords don't match");
            }
            user.get().setPasswordHash(bCryptPasswordEncoder.encode(request.getNewPassword()));
            userRepository.save(user.get());
        }else{
            throw new ResourceNotFoundException("User not found");
        }
    }


}
