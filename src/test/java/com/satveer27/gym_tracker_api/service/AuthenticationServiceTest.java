package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.dto.api.AuthTokens;
import com.satveer27.gym_tracker_api.dto.users.ForgotPasswordRequest;
import com.satveer27.gym_tracker_api.dto.users.UserLoginRequest;
import com.satveer27.gym_tracker_api.dto.users.UserRegisterRequest;
import com.satveer27.gym_tracker_api.entity.RefreshToken;
import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.entity.VerificationToken;
import com.satveer27.gym_tracker_api.enums.Role;
import com.satveer27.gym_tracker_api.enums.TokenType;
import com.satveer27.gym_tracker_api.exception.DuplicateResourceException;
import com.satveer27.gym_tracker_api.exception.InvalidCredentialsException;
import com.satveer27.gym_tracker_api.exception.ResourceNotFoundException;
import com.satveer27.gym_tracker_api.repository.RefreshTokenRepository;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import com.satveer27.gym_tracker_api.repository.VerificationTokenRepository;
import com.satveer27.gym_tracker_api.security.JwtService;
import com.satveer27.gym_tracker_api.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    Authentication authentication;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    // register
    @Test
    void register_shouldCreateUser_whenValidRequest(){
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("test@email.com")).thenReturn(false);

        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        authenticationService.register(request);

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");

        verify(emailVerificationService).createVerificationToken(any(User.class), eq(TokenType.EMAIL_VERIFICATION));
        verify(emailVerificationService).sendVerificationEmail(eq("test@email.com"), any());

        verify(userRepository).save(argThat(user ->
            user.getUsername().equals("testuser") &&
            user.getEmail().equals("test@email.com") &&
            user.getPasswordHash().equals("hashedpassword")
        ));

    }

    @Test
    void register_shouldNotCreateUser_whenUsernameExists(){
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, ()->authenticationService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldNotCreateUser_whenEmailExists(){
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("test@email.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, ()->authenticationService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldHashPassword() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("test@email.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        authenticationService.register(request);

        verify(userRepository).save(argThat(user ->
                user.getPasswordHash().equals("hashedpassword")
        ));
    }

    // login

    @Test
    void login_shouldLoginUser_whenValidRequest(){
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.USER);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtService.generateJwtToken(any(User.class))).thenReturn("token");
        when(jwtService.generateRefreshToken(any(User.class), any(Long.class))).thenReturn("refreshToken");
        when(jwtService.getIssuedAtFromRefresh(any(String.class))).thenReturn(LocalDateTime.now());
        when(jwtService.getExpirationFromRefresh(any(String.class))).thenReturn(LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        AuthTokens response = authenticationService.userLogin(request, null);
        assertEquals("token", response.getToken());
        assertEquals("refreshToken", response.getRefreshToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authentication).getPrincipal();
        verify(jwtService).generateJwtToken(any(User.class));
        verify(jwtService).generateRefreshToken(any(User.class), any(Long.class));
        verify(jwtService).getIssuedAtFromRefresh(any(String.class));
        verify(jwtService).getExpirationFromRefresh(any(String.class));
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void login_shouldThrow_whenInvalidCredentials() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authenticationService.userLogin(request, null));

        verify(refreshTokenRepository, never()).save(any());
        verify(jwtService, never()).generateJwtToken(any());
    }

    @Test
    void login_shouldThrow_whenUserDisabled() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThrows(DisabledException.class,
                () -> authenticationService.userLogin(request, null));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_shouldCleanupOldRefreshToken_whenProvided() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.USER);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        when(jwtService.getRefreshIdFromRefresh("old-refresh-token")).thenReturn(5L);

        when(jwtService.generateJwtToken(any(User.class))).thenReturn("token");
        when(jwtService.generateRefreshToken(any(User.class), any(Long.class))).thenReturn("refreshToken");
        when(jwtService.getIssuedAtFromRefresh(any(String.class))).thenReturn(LocalDateTime.now());
        when(jwtService.getExpirationFromRefresh(any(String.class))).thenReturn(LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        authenticationService.userLogin(request, "old-refresh-token");

        verify(refreshTokenRepository).deleteById(5L);
    }

    @Test
    void login_shouldNotCleanup_whenOldRefreshTokenBlank() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.USER);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtService.generateJwtToken(any(User.class))).thenReturn("token");
        when(jwtService.generateRefreshToken(any(User.class), any(Long.class))).thenReturn("refreshToken");
        when(jwtService.getIssuedAtFromRefresh(any(String.class))).thenReturn(LocalDateTime.now());
        when(jwtService.getExpirationFromRefresh(any(String.class))).thenReturn(LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        authenticationService.userLogin(request, "   ");

        verify(jwtService, never()).getRefreshIdFromRefresh(any());
        verify(refreshTokenRepository, never()).deleteById(any());
    }

    // Reset password
    @Test
    void resetPassword_shouldSucceed_whenValid() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("newPassword");

        User user = new User();
        user.setId(1L);

        VerificationToken token = new VerificationToken();
        token.setId(1L);
        token.setUser(user);
        token.setExpiry(LocalDateTime.now().plusMinutes(10));

        when(verificationTokenRepository.findByTokenAndType("valid-token", TokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        authenticationService.resetPassword(request, "valid-token");

        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("encodedNewPassword")));
        verify(verificationTokenRepository).delete(token);
        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void resetPassword_shouldThrow_whenPasswordsMismatch() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("differentPassword");

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.resetPassword(request, "valid-token"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_shouldThrow_whenTokenInvalid() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("newPassword");

        when(verificationTokenRepository.findByTokenAndType("invalid-token", TokenType.PASSWORD_RESET))
                .thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.resetPassword(request, "invalid-token"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_shouldThrow_whenTokenExpired() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("newPassword");

        VerificationToken token = new VerificationToken();
        token.setId(1L);
        token.setExpiry(LocalDateTime.now().minusMinutes(5));

        when(verificationTokenRepository.findByTokenAndType("expired-token", TokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.resetPassword(request, "expired-token"));

        verify(verificationTokenRepository).delete(token);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_shouldDeleteAllRefreshTokens() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("newPassword");

        User user = new User();
        user.setId(5L);

        VerificationToken token = new VerificationToken();
        token.setId(1L);
        token.setUser(user);
        token.setExpiry(LocalDateTime.now().plusMinutes(10));

        when(verificationTokenRepository.findByTokenAndType("valid-token", TokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);

        authenticationService.resetPassword(request, "valid-token");

        verify(refreshTokenRepository).deleteByUserId(5L);
    }


    // Refresh token
    @Test
    void refreshToken_shouldReturnNewTokens_whenValid() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.USER);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setId(1L);
        storedToken.setUser(user);
        storedToken.setTokenHash(SecurityUtils.hashToken("valid-refresh-token"));
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(jwtService.validateJwtToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.isAccessToken("valid-refresh-token")).thenReturn(false);
        when(jwtService.getRefreshIdFromRefresh("valid-refresh-token")).thenReturn(1L);
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(storedToken));
        when(jwtService.generateJwtToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any(User.class), eq(1L), any(Date.class))).thenReturn("new-refresh-token");

        when(jwtService.getIssuedAtFromRefresh("new-refresh-token")).thenReturn(LocalDateTime.now());

        AuthTokens result = authenticationService.userRefreshToken("valid-refresh-token");

        assertEquals("new-access-token", result.getToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        verify(refreshTokenRepository).save(storedToken);
        verify(jwtService).generateJwtToken(user);
        verify(jwtService).generateRefreshToken(any(User.class), eq(1L), any(Date.class));
    }

    @Test
    void refreshToken_shouldThrow_whenTokenExpired() {
        when(jwtService.validateJwtToken("expired-token")).thenReturn(false);
        when(jwtService.getRefreshIdAllowExpired("expired-token")).thenReturn(5L);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.userRefreshToken("expired-token"));

        verify(refreshTokenRepository).deleteById(5L);
    }

    @Test
    void refreshToken_shouldThrow_whenTokenNotExist() {
        when(jwtService.validateJwtToken("expired-token")).thenReturn(false);
        when(jwtService.getRefreshIdAllowExpired("expired-token")).thenReturn(null);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.userRefreshToken("expired-token"));

        verify(refreshTokenRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void refreshToken_shouldThrow_whenAccessTokenUsedAsRefresh() {
        when(jwtService.validateJwtToken("access-token")).thenReturn(true);
        when(jwtService.isAccessToken("access-token")).thenReturn(true);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.userRefreshToken("access-token"));

        verify(refreshTokenRepository, never()).findById(any());
    }

    @Test
    void refreshToken_shouldThrow_whenRefreshIdNull() {
        when(jwtService.validateJwtToken("bad-token")).thenReturn(true);
        when(jwtService.isAccessToken("bad-token")).thenReturn(false);
        when(jwtService.getRefreshIdFromRefresh("bad-token")).thenReturn(null);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.userRefreshToken("bad-token"));

        verify(refreshTokenRepository, never()).findById(any(Long.class));
    }

    @Test
    void refreshToken_shouldThrow_whenTokenNotFoundInDb() {
        when(jwtService.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtService.isAccessToken("valid-token")).thenReturn(false);
        when(jwtService.getRefreshIdFromRefresh("valid-token")).thenReturn(99L);
        when(refreshTokenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authenticationService.userRefreshToken("valid-token"));

        verify(jwtService, never()).generateJwtToken(any(User.class));
    }

    @Test
    void refreshToken_shouldThrow_whenReuseDetected() {
        User user = new User();
        user.setId(1L);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setId(1L);
        storedToken.setUser(user);
        storedToken.setTokenHash("different-hash");

        when(jwtService.validateJwtToken("reused-token")).thenReturn(true);
        when(jwtService.isAccessToken("reused-token")).thenReturn(false);
        when(jwtService.getRefreshIdFromRefresh("reused-token")).thenReturn(1L);
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(storedToken));

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.userRefreshToken("reused-token"));

        verify(refreshTokenRepository).deleteById(1L);
        verify(jwtService, never()).generateJwtToken(any(User.class));
    }

    @Test
    void refreshToken_shouldUpdateHashAfterRotation() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setId(1L);
        storedToken.setUser(user);
        storedToken.setTokenHash(SecurityUtils.hashToken("valid-refresh-token"));
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(jwtService.validateJwtToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.isAccessToken("valid-refresh-token")).thenReturn(false);
        when(jwtService.getRefreshIdFromRefresh("valid-refresh-token")).thenReturn(1L);
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(storedToken));
        when(jwtService.generateJwtToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any(User.class), eq(1L), any(Date.class))).thenReturn("new-refresh-token");
        when(jwtService.getIssuedAtFromRefresh("new-refresh-token")).thenReturn(LocalDateTime.now());

        AuthTokens result = authenticationService.userRefreshToken("valid-refresh-token");

        verify(refreshTokenRepository).save(argThat(token ->
                token.getTokenHash().equals(SecurityUtils.hashToken("new-refresh-token"))
        ));
        assertEquals("new-refresh-token", result.getRefreshToken());
        assertEquals("new-access-token", result.getToken());
    }

    // Logout
    @Test
    void logout_shouldDeleteSession_whenValidToken() {
        when(jwtService.getRefreshIdAllowExpired("refresh-token")).thenReturn(5L);

        authenticationService.logout("refresh-token");

        verify(refreshTokenRepository).deleteById(5L);
    }

    @Test
    void logout_shouldDoNothing_whenIdIsNull() {
        when(jwtService.getRefreshIdAllowExpired("bad-token")).thenReturn(null);

        authenticationService.logout("bad-token");

        verify(refreshTokenRepository, never()).deleteById(any());
    }

    // Logout all devices
    @Test
    void logoutAll_shouldDeleteAllUserSessions_whenValid() {
        User user = new User();
        user.setId(1L);

        RefreshToken token = new RefreshToken();
        token.setId(5L);
        token.setUser(user);

        when(jwtService.getRefreshIdAllowExpired("refresh-token")).thenReturn(5L);
        when(refreshTokenRepository.findById(5L)).thenReturn(Optional.of(token));

        authenticationService.logoutFromAllDevices("refresh-token");

        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void logoutAll_shouldDoNothing_whenIdIsNull() {
        when(jwtService.getRefreshIdAllowExpired("bad-token")).thenReturn(null);

        authenticationService.logoutFromAllDevices("bad-token");

        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void logoutAll_shouldDoNothing_whenTokenNotFoundInDb() {
        when(jwtService.getRefreshIdAllowExpired("refresh-token")).thenReturn(5L);
        when(refreshTokenRepository.findById(5L)).thenReturn(Optional.empty());

        authenticationService.logoutFromAllDevices("refresh-token");

        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

}
