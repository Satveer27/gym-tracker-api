package com.satveer27.gym_tracker_api.controller;

import com.satveer27.gym_tracker_api.dto.users.UpdatedUserRequest;
import com.satveer27.gym_tracker_api.dto.users.UserRegisterRequest;
import com.satveer27.gym_tracker_api.dto.users.UserResponse;
import com.satveer27.gym_tracker_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegisterRequest request) {
        log.info("action=register_request username={}", request.getUsername());
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("action=getUserById id={}", id);
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<UserResponse> updateUserById(@PathVariable Long id, @Valid @RequestBody UpdatedUserRequest request){
        log.info("action=updateUserById id={}", id);
        UserResponse response = userService.updateUserById(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


}
