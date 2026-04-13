package com.satveer27.gym_tracker_api.security;

import com.satveer27.gym_tracker_api.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@Slf4j
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateJwtToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user, Long id) {
        return generateRefreshToken(user, id,
                new Date(System.currentTimeMillis() + jwtRefreshExpiration));
    }

    public String generateRefreshToken(User user, Long id, Date expiration) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .claim("refresh_id", id)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String getIdFromJwtToken(String token) {
        Claims claims = extractClaimsFromJwtToken(token, false);
        return claims != null ? claims.getSubject() : null;
    }

    public String getRolesFromToken(String token) {
        Claims claims = extractClaimsFromJwtToken(token, false);
        return claims != null ? claims.get("role", String.class) : null;
    }

    public LocalDateTime getIssuedAtFromRefresh(String token) {
        Claims claims = extractClaimsFromJwtToken(token, false);
        return claims != null && claims.get("type") != null ?
                convertToLocalDateTime(claims.getIssuedAt()) : null;
    }

    public LocalDateTime getExpirationFromRefresh(String token) {
        Claims claims = extractClaimsFromJwtToken(token, false);
        return claims != null ?
                convertToLocalDateTime(claims.getExpiration()) : null;
    }

    public Long getRefreshIdFromRefresh(String token) {
        Claims claims = extractClaimsFromJwtToken(token, false);
        return claims != null && claims.get("type") != null ?
                claims.get("refresh_id", Long.class) : null;
    }

    public Long getRefreshIdAllowExpired(String token) {
        Claims claims = extractClaimsFromJwtToken(token, true);
        return claims != null ? claims.get("refresh_id", Long.class) : null;
    }

    public boolean validateJwtToken(String token) {
        Claims claims = extractClaimsFromJwtToken(token, false);
        return claims != null;
    }

    public boolean isAccessToken(String token) {
        Claims claims = extractClaimsFromJwtToken(token, false);
        return claims != null && claims.get("type") == null;
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Claims extractClaimsFromJwtToken(String token, Boolean allowExpired) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }catch (ExpiredJwtException e) {
            if (allowExpired) {
                return e.getClaims();
            }
            log.warn("action=expired_jwt message={}", e.getMessage());
            return null;
        }
        catch (JwtException e){
            log.warn("action=invalid_jwt message={}", e.getMessage());
            return null;
        }
    }



}
