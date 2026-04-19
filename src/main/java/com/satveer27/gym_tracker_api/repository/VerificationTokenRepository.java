package com.satveer27.gym_tracker_api.repository;

import com.satveer27.gym_tracker_api.entity.VerificationToken;
import com.satveer27.gym_tracker_api.enums.TokenType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    public List<VerificationToken> findByUserId(Long userId);
    public Optional<VerificationToken> findByToken(String token);
    public Optional<VerificationToken> findByUserIdAndType(Long id, TokenType type);
    public Optional<VerificationToken> findByTokenAndType(String token, TokenType type);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);

    @Modifying
    @Transactional
    void deleteByUserIdAndType(Long userId, TokenType type);

    @Modifying
    @Transactional
    void deleteByExpiryBefore(LocalDateTime now);
}
