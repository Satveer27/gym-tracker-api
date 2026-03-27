package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.dto.users.*;
import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.enums.Role;
import com.satveer27.gym_tracker_api.exception.*;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_shouldCreateUser_whenValidRequest(){
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("test@email.com")).thenReturn(false);

        when(bCryptPasswordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        UserResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@email.com", response.getEmail());
        verify(userRepository).save(any(User.class));
        verify(bCryptPasswordEncoder).encode("password123");

    }

    @Test
    void register_shouldNotCreateUser_whenUsernameExists(){
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, ()->userService.register(request));
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
        assertThrows(DuplicateResourceException.class, ()->userService.register(request));
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
        when(bCryptPasswordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        userService.register(request);

        verify(userRepository).save(argThat(user ->
                user.getPasswordHash().equals("hashedpassword")
        ));
    }

    @Test
    void register_shouldNotReturnPasswordInResponse() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("test@email.com")).thenReturn(false);
        when(bCryptPasswordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = userService.register(request);
        assertNotNull(response.getId());
        assertNotNull(response.getUsername());
        assertNotNull(response.getEmail());
    }

    @Test
    void findUser_shouldReturnUser_whenUserExists() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@email.com");
        user.setPasswordHash("password123");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserResponse result = userService.getUserById(1L);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@email.com", result.getEmail());
        verify(userRepository).findById(1L);

    }

    @Test
    void findUser_shouldNotReturnUser_whenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, ()->userService.getUserById(1L));
    }

    @Test
    void updateUser_shouldUpdateUser() {
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setUsername("testuser1");
        request.setEmail("test@email1.com");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@email.com");
        user.setPasswordHash("password123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("testuser1")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("test@email1.com")).thenReturn(false);

        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.updateUserById(1L, request);

        assertEquals("testuser1", response.getUsername());
        assertEquals("test@email1.com", response.getEmail());
    }

    @Test
    void throwException_whenRoleIsAdmin() {
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setRole(Role.ADMIN);

        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedActionException.class,
                () -> userService.updateUserById(1L, request));
    }

    @Test
    void throwDuplicate_whenUserExists() {
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
    void throwDuplicate_whenEmailExists() {
        UpdatedUserRequest request = new UpdatedUserRequest();
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
                () -> userService.updateUserById(1L, request));
    }

    @Test
    void returnSameUser_whenAllFieldEmptyString() {
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setUsername("");
        request.setEmail("");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");
        user.setRole(Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.updateUserById(1L, request);

        assertEquals("testuser", response.getUsername());
        assertEquals("testuser@gmail.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
    }

    @Test
    void throwException_whenUserNotFound() {
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setUsername("testuser");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserById(1L, request));
    }

    @Test
    void returnSameUser_whenAllFieldNullString() {
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setUsername(null);
        request.setEmail(null);
        request.setRole(null);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");
        user.setRole(Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.updateUserById(1L, request);

        assertEquals("testuser", response.getUsername());
        assertEquals("testuser@gmail.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
    }

    @Test
    void returnUserWithChangeEmail_whenUpdateUser() {
        UpdatedUserRequest request = new UpdatedUserRequest();
        request.setEmail("testuser1@gmail.com");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@gmail.com");
        user.setRole(Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("testuser1@gmail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.updateUserById(1L, request);

        assertEquals("testuser", response.getUsername());
        assertEquals("testuser1@gmail.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
    }

    @Test
    void updatePassword_success() {
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
    }

    @Test
    void updatePassword_throwsWhenPasswordsMismatch() {
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
    void updatePassword_throwsWhenOldPasswordWrong() {
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
    void updatePassword_throwsWhenUserNotFound() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmNewPassword("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserPassword(1L, request));
    }

    @Test
    void deleteUser_success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUserById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_throwsWhenUserNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUserById(1L));
    }

    @Test
    void getAllUsers_returnsPagedResults() {
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
    void getAllUsers_returnsEmptyPage() {
        Page<User> page = new PageImpl<>(List.of(), PageRequest.of(0, 30), 0);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        GetAllUsersResponse response = userService.getAllUser(
                PageRequest.of(0, 30), null, null, null, null, null);

        assertEquals(0, response.getUsers().size());
        assertEquals(0, response.getTotalItems());
    }
}
