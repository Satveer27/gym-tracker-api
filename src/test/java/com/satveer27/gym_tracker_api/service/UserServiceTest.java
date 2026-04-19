package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.dto.users.*;
import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.enums.Role;
import com.satveer27.gym_tracker_api.enums.TokenType;
import com.satveer27.gym_tracker_api.exception.*;
import com.satveer27.gym_tracker_api.repository.RefreshTokenRepository;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    private UserService userService;

    // Get user By id
    @Test
    void getUserById_shouldReturnUser_whenUserExists(){
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@email.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserById(1L);

        assertEquals("testuser", result.getUsername());
        assertEquals("test@email.com", result.getEmail());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_shouldThrow_whenUserNotFound(){
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, ()->userService.getUserById(1L));
    }

    //Updated user by id
    @Test
    void updateUserById_shouldUpdateUsername_whenValid(){
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setUsername("testuser1");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@email.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("testuser1")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.updateUserById(1L, request);

        assertEquals("testuser1", response.getUsername());
        assertEquals("test@email.com", response.getEmail());
    }

    @Test
    void updateUserById_shouldThrow_whenRoleIsAdmin() {
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setRole(Role.ADMIN);

        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedActionException.class,
                () -> userService.updateUserById(1L, request));
    }

    @Test
    void updateUserById_shouldThrow_whenDuplicateUsername() {
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setUsername("testuser");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser1");
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(DuplicateResourceException.class,
                () -> userService.updateUserById(1L, request));
    }

    @Test
    void updateUserById_shouldThrow_whenUserNotFound() {
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setUsername("testuser");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserById(1L, request));
    }

    @Test
    void updateUserById_shouldNotThrowDuplicate_whenSameUsername() {
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setUsername("testuser");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@email.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.updateUserById(1L, request);

        assertEquals("testuser", response.getUsername());
    }

    // Updated user by Id admin
    @Test
    void updateUserByIdAdmin_shouldThrow_whenUserNotFound() {
        UpdateUserRequestAdmin request = new UpdateUserRequestAdmin();
        request.setUsername("testuser");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserByIdAdmin(1L, request));
    }


    @Test
    void updateUserByIdAdmin_shouldThrow_whenDuplicateEmail() {
        UpdateUserRequestAdmin request = new UpdateUserRequestAdmin();
        request.setUsername("testuser");
        request.setEmail("testuser@gmail.com");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser1@gmail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("testuser@gmail.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> userService.updateUserByIdAdmin(1L, request));
    }



    @Test
    void updateUserByIdAdmin_shouldUpdateEmailAndRole() {
        UpdateUserRequestAdmin request = new UpdateUserRequestAdmin();
        request.setEmail("testuser1@gmail.com");
        request.setRole(Role.ADMIN);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");
        user.setRole(Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("testuser1@gmail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.updateUserByIdAdmin(1L, request);

        assertEquals("testuser", response.getUsername());
        assertEquals("testuser1@gmail.com", response.getEmail());
        assertEquals(Role.ADMIN, response.getRole());

    }

    @Test
    void updateUserByIdAdmin_shouldResetVerificationAndDeleteTokens_whenEmailChanged() {
        UpdateUserRequestAdmin request = new UpdateUserRequestAdmin();
        request.setEmail("testuser1@gmail.com");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");
        user.setEmailVerified(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("testuser1@gmail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUserByIdAdmin(1L, request);

        assertFalse(user.isEmailVerified());
        verify(refreshTokenRepository).deleteByUserId(1L);
        verify(emailVerificationService).createVerificationToken(any(User.class), eq(TokenType.EMAIL_VERIFICATION));
        verify(emailVerificationService).sendVerificationEmail(eq("testuser1@gmail.com"), any());
    }

    @Test
    void updateUserByIdAdmin_shouldNotSendEmail_whenEmailUnchanged() {
        UpdateUserRequestAdmin request = new UpdateUserRequestAdmin();
        request.setEmail("testuser@gmail.com");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUserByIdAdmin(1L, request);

        verify(refreshTokenRepository, never()).deleteByUserId(any());
        verify(emailVerificationService, never()).createVerificationToken(any(), eq(TokenType.EMAIL_VERIFICATION));
        verify(emailVerificationService, never()).sendVerificationEmail(any(), any());
    }


    // updated password
    @Test
    void updatePassword_shouldSucceed_whenValid() {
        User user = new User();
        user.setId(1L);
        when(bCryptPasswordEncoder.encode("oldPassword")).thenReturn("hashedOldPassword");
        user.setPasswordHash(bCryptPasswordEncoder.encode("oldPassword"));

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("oldPassword", user.getPasswordHash())).thenReturn(true);
        when(bCryptPasswordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUserPassword(1L, request);

        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void updatePassword_shouldThrow_whenPasswordsMismatch() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("differentPassword");

        assertThrows(PasswordMismatchException.class,
                () -> userService.updateUserPassword(1L, request));
    }

    @Test
    void updatePassword_shouldThrow_whenOldPasswordWrong() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("encodedOldPassword");

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.updateUserPassword(1L, request));
    }

    @Test
    void updatePassword_shouldThrow_whenUserNotFound() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserPassword(1L, request));
    }

    @Test
    void updatePassword_shouldHashNewPassword() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("encodedOldPassword");

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(bCryptPasswordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUserPassword(1L, request);

        verify(userRepository).save(argThat(u ->
                u.getPasswordHash().equals("encodedNewPassword")
        ));
    }

    // delete users
    @Test
    void deleteUser_shouldSucceed_whenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUserById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrow_whenUserNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUserById(1L));
    }


    // get all users
    @Test
    void getAllUsers_shouldReturnPagedResults() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("testuser1");
        user1.setEmail("test1@email.com");
        user1.setRole(Role.USER);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("testuser2");
        user2.setEmail("test2@email.com");
        user2.setRole(Role.TRAINER);

        Page<User> page = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 30), 2);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        GetAllUsersResponse response = userService.getAllUser(
                PageRequest.of(0, 30), null, null, null, null, null);

        assertEquals(2, response.getUsers().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotalItems());
        assertFalse(response.isHasNext());
        assertFalse(response.isHasPrevious());
    }

    @Test
    void getAllUsers_shouldReturnEmptyPage() {
        Page<User> page = new PageImpl<>(List.of(), PageRequest.of(0, 30), 0);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        GetAllUsersResponse response = userService.getAllUser(
                PageRequest.of(0, 30), null, null, null, null, null);

        assertEquals(0, response.getUsers().size());
        assertEquals(0, response.getTotalItems());
    }
}
