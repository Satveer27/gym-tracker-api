package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.dto.api.AuthTokens;
import com.satveer27.gym_tracker_api.dto.users.ForgotPasswordRequest;
import com.satveer27.gym_tracker_api.dto.users.UserLoginRequest;
import com.satveer27.gym_tracker_api.dto.users.UserRegisterRequest;
import com.satveer27.gym_tracker_api.entity.RefreshToken;
import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.entity.VerificationToken;
import com.satveer27.gym_tracker_api.enums.TokenType;
import com.satveer27.gym_tracker_api.exception.DuplicateResourceException;
import com.satveer27.gym_tracker_api.exception.InvalidCredentialsException;
import com.satveer27.gym_tracker_api.exception.ResourceNotFoundException;
import com.satveer27.gym_tracker_api.repository.RefreshTokenRepository;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import com.satveer27.gym_tracker_api.repository.VerificationTokenRepository;
import com.satveer27.gym_tracker_api.security.JwtService;
import com.satveer27.gym_tracker_api.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final VerificationTokenRepository verificationTokenRepository;

    // Service methods
    public AuthTokens userLogin(UserLoginRequest userLoginRequest){
        log.info("action=user_login username={}", userLoginRequest.getUsername());
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userLoginRequest.getUsername(),
                        userLoginRequest.getPassword())
        );

        User user = (User) auth.getPrincipal();

        String token = jwtService.generateJwtToken(user);
        String refreshToken = createRefreshTokenForUser(user);

        log.info("action=user_login_success username={}", user.getUsername());
        return AuthTokens.from(token, refreshToken);
    }

    public void resetPassword(ForgotPasswordRequest resetPasswordRequest, String verificationToken){
        if(!resetPasswordRequest.getNewPassword().equals(resetPasswordRequest.getConfirmNewPassword())){
            throw new InvalidCredentialsException("New passwords do not match");
        }
        VerificationToken result = verificationTokenRepository.findByTokenAndType(verificationToken, TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid verification token"));

        if (result.getExpiry().isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(result);
            throw new InvalidCredentialsException("Token expired");
        }

        User user = result.getUser();
        user.setPasswordHash(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        user.setLastPasswordResetAt(LocalDateTime.now());
        userRepository.save(user);

        verificationTokenRepository.delete(result);
        refreshTokenRepository.deleteByUserId(user.getId());
        log.info("action=user_password_reset_success userId={}", user.getId());
    }

    public void register(UserRegisterRequest request) {
        log.debug("action=register_user username={}", request.getUsername());

        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername().toLowerCase());
        user.setEmail(request.getEmail().toLowerCase());
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPasswordHash(encodedPassword);
        User saved = userRepository.save(user);
        log.info("action=register_user user_id={} username={} status=success",
                saved.getId(), saved.getUsername());

        String emailToken = emailVerificationService.createVerificationToken(saved, TokenType.EMAIL_VERIFICATION);
        emailVerificationService.sendVerificationEmail(saved.getEmail(), emailToken);
    }

    public AuthTokens userRefreshToken(String refreshToken){
         log.info("action=generate_new_refresh_token");
         if(!jwtService.validateJwtToken(refreshToken)){
             Long id = jwtService.getRefreshIdAllowExpired(refreshToken);
             if (id != null) {
                 refreshTokenRepository.deleteById(id);
                 log.info("action=expired_refresh_token_deleted sessionId={}", id);
             }
             throw new InvalidCredentialsException("Refresh token expired");
         };

         if(jwtService.isAccessToken(refreshToken)){
             throw new InvalidCredentialsException("Invalid credentials");
         }

         Long id = jwtService.getRefreshIdFromRefresh(refreshToken);
         if(id == null){
             throw new InvalidCredentialsException("Invalid refresh token");
         }

         Optional<RefreshToken> result = refreshTokenRepository.findById(id);
         if(!result.isPresent()){
             throw new ResourceNotFoundException("Refresh token not found");
         }
         if(!SecurityUtils.hashToken(refreshToken).equals(result.get().getTokenHash())){
             log.warn("action=refresh_token_reuse_detected id={} user_id={}", id, result.get().getUser().getId());
             refreshTokenRepository.deleteById(id);
             log.info("action=delete_refresh_token id={}", id);
             throw new InvalidCredentialsException("Refresh token reuse detected");
         }
         String accessToken = jwtService.generateJwtToken(result.get().getUser());
         String refreshTokenUpdated = jwtService.generateRefreshToken(result.get().getUser(), id,
                 convertToDate(result.get().getExpiresAt()));

         result.get().setTokenHash(SecurityUtils.hashToken(refreshTokenUpdated));
         result.get().setIssuedAt(jwtService.getIssuedAtFromRefresh(refreshTokenUpdated));
         refreshTokenRepository.save(result.get());
         log.info("action=update_refresh_token id={}", id);

         return AuthTokens.from(accessToken, refreshTokenUpdated);
     }

    public void logout(String refreshToken){
        log.info("action=logout_user");
        Long id = jwtService.getRefreshIdAllowExpired(refreshToken);
        if (id != null) {
            refreshTokenRepository.deleteById(id);
            log.info("action=logout_success sessionId={}", id);
        }
    }

    public void logoutFromAllDevices(String refreshToken){
        log.info("action=logout_all_request");
        Long id  = jwtService.getRefreshIdAllowExpired(refreshToken);
        if(id != null){
            Optional<RefreshToken> token = refreshTokenRepository.findById(id);
            if(token.isPresent()){
                refreshTokenRepository.deleteByUserId(token.get().getUser().getId());
                log.info("action=logout_all_success sessionId={} userId={}", id,  token.get().getUser().getId());
            }
        }
    }

    //utils
    private String createRefreshTokenForUser(User user) {
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash("temporary");
        entity.setIssuedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(entity);

        String refreshToken = jwtService.generateRefreshToken(user, entity.getId());

        entity.setTokenHash(SecurityUtils.hashToken(refreshToken));
        entity.setIssuedAt(jwtService.getIssuedAtFromRefresh(refreshToken));
        entity.setExpiresAt(jwtService.getExpirationFromRefresh(refreshToken));
        refreshTokenRepository.save(entity);

        log.info("action=refresh_token_created userId={} sessionId={}", user.getId(), entity.getId());

        return refreshToken;
    }


    private Date convertToDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
