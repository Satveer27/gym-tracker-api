package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.dto.users.UserRegisterRequest;
import com.satveer27.gym_tracker_api.dto.users.UserResponse;
import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.exception.DuplicateResourceException;
import com.satveer27.gym_tracker_api.exception.ResourceNotFoundException;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);

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
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, ()->userService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldNotCreateUser_whenEmailExists(){
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@email.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, ()->userService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldHashPassword() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@email.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);
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

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);
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



}
