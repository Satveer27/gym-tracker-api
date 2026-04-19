package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.entity.VerificationToken;
import com.satveer27.gym_tracker_api.enums.TokenType;
import com.satveer27.gym_tracker_api.exception.InvalidCredentialsException;
import com.satveer27.gym_tracker_api.exception.ResourceNotFoundException;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import com.satveer27.gym_tracker_api.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class EmailVerificationServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailVerificationService emailVerificationService;


    // create verification token
    @Test
    void createVerificationToken_shouldDeleteExistingAndCreateNew() {
        User user = new User();
        user.setId(1L);

        when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> {
            VerificationToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        String token = emailVerificationService.createVerificationToken(user, TokenType.EMAIL_VERIFICATION);

        assertNotNull(token);
        verify(verificationTokenRepository).deleteByUserIdAndType(1L, TokenType.EMAIL_VERIFICATION);
        verify(verificationTokenRepository).save(argThat(t ->
                t.getUser().equals(user) &&
                t.getType() == TokenType.EMAIL_VERIFICATION &&
                t.getExpiry().isAfter(LocalDateTime.now())
        ));
    }

    // verify email
    @Test
    void verifyEmail_shouldVerifyUser_whenTokenValid() {
        User user = new User();
        user.setId(1L);
        user.setEmailVerified(false);

        VerificationToken token = new VerificationToken();
        token.setId(1L);
        token.setUser(user);
        token.setType(TokenType.EMAIL_VERIFICATION);
        token.setExpiry(LocalDateTime.now().plusMinutes(10));

        when(verificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        emailVerificationService.verifyEmail("valid-token");

        assertTrue(user.isEmailVerified());
        verify(userRepository).save(user);
        verify(verificationTokenRepository).delete(token);
    }



    @Test
    void verifyEmail_shouldThrow_whenTokenNotFound() {
        when(verificationTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> emailVerificationService.verifyEmail("invalid-token"));
    }

    @Test
    void verifyEmail_shouldThrow_whenWrongTokenType() {
        VerificationToken token = new VerificationToken();
        token.setType(TokenType.PASSWORD_RESET);

        when(verificationTokenRepository.findByToken("wrong-type")).thenReturn(Optional.of(token));

        assertThrows(InvalidCredentialsException.class, () -> emailVerificationService.verifyEmail("wrong-type"));
    }

    @Test
    void verifyEmail_shouldThrow_whenTokenExpired() {
        VerificationToken token = new VerificationToken();
        token.setId(1L);
        token.setType(TokenType.EMAIL_VERIFICATION);
        token.setExpiry(LocalDateTime.now().minusMinutes(5));

        when(verificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(InvalidCredentialsException.class,
                () -> emailVerificationService.verifyEmail("expired-token"));

        verify(verificationTokenRepository).delete(token);
        verify(userRepository, never()).save(any());
    }

    // send email
    @Test
    void sendEmail_shouldDoNothing_whenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("unknown@email.com")).thenReturn(Optional.empty());

        emailVerificationService.sendEmail("unknown@email.com", TokenType.EMAIL_VERIFICATION);

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldDoNothing_whenAlreadyVerified() {
        User user = new User();
        user.setId(1L);
        user.setEmailVerified(true);

        when(userRepository.findByEmailIgnoreCase("test@email.com")).thenReturn(Optional.of(user));

        emailVerificationService.sendEmail("test@email.com", TokenType.EMAIL_VERIFICATION);

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldDoNothing_whenWithinCooldown() {
        User user = new User();
        user.setId(1L);
        user.setEmailVerified(false);

        VerificationToken existing = new VerificationToken();
        existing.setIssuedAt(LocalDateTime.now().minusSeconds(60));

        when(userRepository.findByEmailIgnoreCase("test@email.com")).thenReturn(Optional.of(user));
        when(verificationTokenRepository.findByUserIdAndType(1L, TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(existing));

        emailVerificationService.sendEmail("test@email.com", TokenType.EMAIL_VERIFICATION);

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldSendVerificationEmail_whenCooldownPassed() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setEmailVerified(false);

        VerificationToken existing = new VerificationToken();
        existing.setIssuedAt(LocalDateTime.now().minusSeconds(180));

        when(userRepository.findByEmailIgnoreCase("test@email.com")).thenReturn(Optional.of(user));
        when(verificationTokenRepository.findByUserIdAndType(1L, TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(existing));
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> {
            VerificationToken token = invocation.getArgument(0);
            token.setId(2L);
            return token;
        });

        emailVerificationService.sendEmail("test@email.com", TokenType.EMAIL_VERIFICATION);

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldSendPasswordResetEmail_whenValid() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setEmailVerified(true);

        when(userRepository.findByEmailIgnoreCase("test@email.com")).thenReturn(Optional.of(user));
        when(verificationTokenRepository.findByUserIdAndType(1L, TokenType.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> {
            VerificationToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        emailVerificationService.sendEmail("test@email.com", TokenType.PASSWORD_RESET);

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldDoNothing_whenPasswordResetWithinCooldown() {
        User user = new User();
        user.setId(1L);
        user.setLastPasswordResetAt(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByEmailIgnoreCase("test@email.com")).thenReturn(Optional.of(user));

        emailVerificationService.sendEmail("test@email.com", TokenType.PASSWORD_RESET);

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_shouldSend_whenNoExistingToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setEmailVerified(false);

        when(userRepository.findByEmailIgnoreCase("test@email.com")).thenReturn(Optional.of(user));
        when(verificationTokenRepository.findByUserIdAndType(1L, TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());
        when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> {
            VerificationToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        emailVerificationService.sendEmail("test@email.com", TokenType.EMAIL_VERIFICATION);

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

}
