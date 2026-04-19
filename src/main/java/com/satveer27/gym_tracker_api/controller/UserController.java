package com.satveer27.gym_tracker_api.controller;

import com.satveer27.gym_tracker_api.dto.users.*;
import com.satveer27.gym_tracker_api.exception.UnauthorizedActionException;
import com.satveer27.gym_tracker_api.service.UserService;
import com.satveer27.gym_tracker_api.utils.CookieCreatorHelper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('USER', 'COACH', 'ADMIN')")
    public ResponseEntity<UserResponse> getMe(){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("action=getUserById id={}", id);
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @PatchMapping("/admin/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<UserResponse> updateUserByIdAdmin(@PathVariable Long id, @Valid @RequestBody UpdateUserRequestAdmin request){
        log.info("action=updateUserByIdAdmin id={}", id);
        UserResponse response = userService.updateUserByIdAdmin(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/me/update")
    public ResponseEntity<UserResponse> updateUserByMe(@Valid @RequestBody UpdatedUserRequest request){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("action=updateUserById userId={}", userId);
        UserResponse response = userService.updateUserById(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/updatedPassword")
    public ResponseEntity<Void> updatedPasswordById(@Valid @RequestBody UpdatePasswordRequest request, HttpServletResponse response) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("action=updatedPasswordById id={}", userId);
        userService.updateUserPassword(userId, request);
        CookieCreatorHelper.clearAuthCookies(response);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Void> deleteUserByIdAdmin(@PathVariable Long id){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(userId.equals(id)){
            throw new UnauthorizedActionException("Cannot delete your own account");
        }
        log.info("action=deleteUserByIdAdmin id={}", id);
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/me")
    public ResponseEntity<Void> deleteUserById(HttpServletResponse response){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("action=deleteUserById id={}", userId);
        userService.deleteUserById(userId);
        CookieCreatorHelper.clearAuthCookies(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/allUsers")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
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
