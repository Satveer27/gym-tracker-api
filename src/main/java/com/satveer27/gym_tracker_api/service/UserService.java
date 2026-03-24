package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.dto.users.UserRegisterRequest;
import com.satveer27.gym_tracker_api.dto.users.UserResponse;
import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.exception.DuplicateResourceException;
import com.satveer27.gym_tracker_api.exception.ResourceNotFoundException;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserResponse register(UserRegisterRequest request) {
        log.debug("action=register_user username={}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
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


}
