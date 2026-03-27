package com.satveer27.gym_tracker_api.controller;

import com.satveer27.gym_tracker_api.dto.users.*;
import com.satveer27.gym_tracker_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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

    @PatchMapping("/updatedPassword/{id}")
    public ResponseEntity<Void> updatedPasswordById(@PathVariable Long id, @Valid @RequestBody UpdatePasswordRequest request) {
        log.info("action=updatedPasswordById id={}", id);
        userService.updateUserPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id){
        log.info("action=deleteUserById id={}", id);
        userService.deleteUserById(id);
        return  ResponseEntity.noContent().build();
    }

    @GetMapping("/allUsers")
    public ResponseEntity<GetAllUsersResponse> getAllUsers(
            @PageableDefault(page = 0, size=30, sort="id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end){
        log.info("action=getAllUsers");
        return ResponseEntity.status(HttpStatus.OK).body(userService.getAllUser(pageable, role, username, email, start, end));
    }

}
