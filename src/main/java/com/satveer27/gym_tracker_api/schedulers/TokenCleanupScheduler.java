package com.satveer27.gym_tracker_api.schedulers;

import com.satveer27.gym_tracker_api.repository.RefreshTokenRepository;
import com.satveer27.gym_tracker_api.repository.UserRepository;
import com.satveer27.gym_tracker_api.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;

    @Scheduled(fixedRate = 86400000)
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("action=expired_tokens_purged");
    }

    @Scheduled(cron = "0 0 0 * * MON")
    public void purgeExpiredVerificationTokens() {
        verificationTokenRepository.deleteByExpiryBefore(LocalDateTime.now());
        log.info("action=expired_verification_tokens_purged");
    }

    @Scheduled(cron = "0 0 0 * * MON")
    public void purgeUnverifiedAccounts() {
        userRepository.deleteByEmailVerifiedFalseAndCreatedAtBefore(LocalDateTime.now().minusWeeks(1));
        log.info("action=unverified_accounts_purged");
    }

}
