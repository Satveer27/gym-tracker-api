package com.satveer27.gym_tracker_api.controller;

import com.satveer27.gym_tracker_api.dto.api.ApiResponse;
import com.satveer27.gym_tracker_api.dto.api.AuthTokens;
import com.satveer27.gym_tracker_api.dto.auth.ResendVerificationRequest;
import com.satveer27.gym_tracker_api.dto.users.ForgotPasswordRequest;
import com.satveer27.gym_tracker_api.dto.users.UserLoginRequest;
import com.satveer27.gym_tracker_api.dto.users.UserRegisterRequest;
import com.satveer27.gym_tracker_api.enums.TokenType;
import com.satveer27.gym_tracker_api.service.AuthenticationService;
import com.satveer27.gym_tracker_api.service.EmailVerificationService;
import com.satveer27.gym_tracker_api.utils.CookieCreatorHelper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @Valid @RequestBody UserLoginRequest userLoginRequest,
            HttpServletResponse response) {
        log.info("action=login_request username={}", userLoginRequest.getUsername());
        AuthTokens authTokens = authenticationService.userLogin(userLoginRequest);
        CookieCreatorHelper.setAuthCookies(response, authTokens);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.from("Login successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody UserRegisterRequest request) {
        log.info("action=register_request username={}", request.getUsername());
        authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.from("Registration successful, email sent to " + request.getEmail()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshUser(
            @CookieValue(name = "refresh_token", required = true) String refreshToken,
            HttpServletResponse response) {
        log.info("action=refresh_request");
        AuthTokens authTokens = authenticationService.userRefreshToken(refreshToken);
        CookieCreatorHelper.setAuthCookies(response, authTokens);
        log.info("action=refresh_request_success");
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.from("refresh successful"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @CookieValue(name = "refresh_token", required = true) String refresh_token,
            HttpServletResponse response){
        log.info("action=logout_request");
        authenticationService.logout(refresh_token);
        CookieCreatorHelper.clearAuthCookies(response);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.from("Logout successful"));
    }


    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse> logoutAll(
            @CookieValue(name = "refresh_token", required = true) String refreshToken,
            HttpServletResponse response) {
        log.info("action=logout_all_request");
        authenticationService.logoutFromAllDevices(refreshToken);
        CookieCreatorHelper.clearAuthCookies(response);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.from("Logged out from all devices"));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verifyEmail(@RequestParam("token") String token) {
        log.info("action=verify_email_request");
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.from("Email verified successfully"));
    }

    @PostMapping("/resend-email-verification")
    public ResponseEntity<ApiResponse> resendEmailVerification(@Valid @RequestBody ResendVerificationRequest request){
        log.info("action=resend_email_verification");
        emailVerificationService.sendEmail(request.getEmail(), TokenType.EMAIL_VERIFICATION);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.from("If the email exists, a verification link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestParam("token") String token, @Valid @RequestBody ForgotPasswordRequest request){
        log.info("action=reset_password_request");
        authenticationService.resetPassword(request, token);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.from("Password reset successful"));
    }

    @PostMapping("/forget-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ResendVerificationRequest request){
        log.info("action=forgot_password_request");
        emailVerificationService.sendEmail(request.getEmail(), TokenType.PASSWORD_RESET);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.from("If the email exists, a reset link has been sent"));
    }
}
