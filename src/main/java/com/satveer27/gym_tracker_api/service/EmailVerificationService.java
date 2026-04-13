package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.entity.VerificationToken;
import com.satveer27.gym_tracker_api.enums.TokenType;
import com.satveer27.gym_tracker_api.exception.InvalidCredentialsException;
import com.satveer27.gym_tracker_api.exception.ResourceNotFoundException;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import com.satveer27.gym_tracker_api.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationService {
    private final JavaMailSender mailSender;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public String createVerificationToken(User user, TokenType type) {
        verificationTokenRepository.deleteByUserIdAndType(user.getId(), type);
        log.info("action=delete_verification_token_if_exist userid={} type={}", user.getId(), type.toString());

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setExpiry(LocalDateTime.now().plusMinutes(15));
        verificationToken.setType(type);

        verificationTokenRepository.save(verificationToken);
        log.info("action=create_verification_token userid={}", user.getId());
        return verificationToken.getToken();
    }

    public void sendVerificationEmail(String to, String verificationToken) {
        String link = baseUrl + "/api/v1/auth/verify?token=" + verificationToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Verify your email for gym app");
        message.setText("Click the link to verify your email:\n\n" + link +
                "\n\nThis link expires in 15 minutes.");
        mailSender.send(message);
        log.info("action=verification_email_sent to={}", to);

    }

    public void verifyEmail(String verificationToken) {
        VerificationToken result = verificationTokenRepository.findByToken(verificationToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

        if (result.getType() != TokenType.EMAIL_VERIFICATION) {
            throw new InvalidCredentialsException("Invalid token type");
        }

        if (result.getExpiry().isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(result);
            log.info("action=delete_verification_token id={}", result.getId());
            throw new InvalidCredentialsException("Verification token expired");
        }

        User user = result.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("action=update_email_verified userId={}", user.getId());


        verificationTokenRepository.delete(result);
        log.info("action=email_verified userId={}", user.getId());
    }

    public void sendEmail(String email, TokenType type) {
        User emailResult = userRepository.findByEmailIgnoreCase(email)
                .orElse(null);

        if (emailResult == null) {
            return;
        }

        if(emailResult.getLastPasswordResetAt() != null && Duration.between(emailResult.getLastPasswordResetAt(), LocalDateTime.now()).toMinutes() < 10){
            return;
        }

        if (type == TokenType.EMAIL_VERIFICATION && emailResult.isEmailVerified()) {
            return;
        }


        Optional<VerificationToken> existing = verificationTokenRepository.findByUserIdAndType(emailResult.getId(), type);
        if (existing.isPresent()) {
            long secondsSinceIssued = Duration.between(
                    existing.get().getIssuedAt(), LocalDateTime.now()
            ).getSeconds();

            if (secondsSinceIssued < 120) {
                return;
            }
        }
        String verificationTokenCreated = createVerificationToken(emailResult, type);
        if(type.equals(TokenType.EMAIL_VERIFICATION)) {
            sendVerificationEmail(emailResult.getEmail(), verificationTokenCreated);
        }
        else{
            sendForgetPasswordEmail(emailResult.getEmail(), verificationTokenCreated);
        }

        log.info("action=verification_email_resent userId={}", emailResult.getId());
    }

    public void sendForgetPasswordEmail(String to, String verificationToken){
        String link = baseUrl + "/api/v1/auth/reset-password?token=" + verificationToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Reset password link");
        message.setText("Click the link to reset your password:\n\n" + link +
                "\n\nThis link expires in 15 minutes.");
        mailSender.send(message);
        log.info("action=forgot_password_email_sent to={}", to);
    }

}
