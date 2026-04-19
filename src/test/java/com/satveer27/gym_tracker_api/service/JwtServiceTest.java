package com.satveer27.gym_tracker_api.service;

import com.satveer27.gym_tracker_api.entity.User;
import com.satveer27.gym_tracker_api.enums.Role;
import com.satveer27.gym_tracker_api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {
    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        setField("jwtSecret", "a]very-secure-secret-key-that-is-at-least-32-characters-long");
        setField("jwtExpiration", 900000L); // 15 minutes
        setField("jwtRefreshExpiration", 604800000L); // 7 days

        jwtService.init();
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = JwtService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(jwtService, value);
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.USER);
        return user;
    }

    // Generate jwt token
    @Test
    void generateJwtToken_shouldReturnNonNullToken() {
        String token = jwtService.generateJwtToken(createUser());
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateJwtToken_shouldContainCorrectSubject() {
        String token = jwtService.generateJwtToken(createUser());
        assertEquals("1", jwtService.getIdFromJwtToken(token));
    }

    @Test
    void generateJwtToken_shouldContainCorrectRole() {
        String token = jwtService.generateJwtToken(createUser());
        assertEquals("USER", jwtService.getRolesFromToken(token));
    }

    @Test
    void generateJwtToken_shouldBeAccessToken() {
        String token = jwtService.generateJwtToken(createUser());
        assertTrue(jwtService.isAccessToken(token));
    }

    // validate jwt token
    @Test
    void validateJwtToken_shouldReturnTrue_whenTokenValid() {
        String token = jwtService.generateJwtToken(createUser());
        assertTrue(jwtService.validateJwtToken(token));
    }

    @Test
    void validateJwtToken_shouldReturnFalse_whenTokenTampered() {
        String token = jwtService.generateJwtToken(createUser());
        String tampered = token.substring(0, token.length() - 5) + "xxxxx";
        assertFalse(jwtService.validateJwtToken(tampered));
    }

    @Test
    void validateJwtToken_shouldReturnFalse_whenTokenExpired() throws Exception {
        setField("jwtExpiration", 0L);
        jwtService.init();

        String token = jwtService.generateJwtToken(createUser());

        Thread.sleep(10);

        assertFalse(jwtService.validateJwtToken(token));
    }

    @Test
    void validateJwtToken_shouldReturnFalse_whenTokenGarbage() {
        assertFalse(jwtService.validateJwtToken("not.a.real.token"));
    }

    //  is access token
    @Test
    void isAccessToken_shouldReturnTrue_forAccessToken() {
        String token = jwtService.generateJwtToken(createUser());
        assertTrue(jwtService.isAccessToken(token));
    }

    @Test
    void isAccessToken_shouldReturnFalse_forRefreshToken() {
        String token = jwtService.generateRefreshToken(createUser(), 1L);
        assertFalse(jwtService.isAccessToken(token));
    }

    // get id from jwt token
    @Test
    void getIdFromJwtToken_shouldReturnUserId() {
        User user = createUser();
        user.setId(42L);
        String token = jwtService.generateJwtToken(user);
        assertEquals("42", jwtService.getIdFromJwtToken(token));
    }

    @Test
    void getIdFromJwtToken_shouldReturnNull_whenTokenInvalid() {
        assertNull(jwtService.getIdFromJwtToken("invalid-token"));
    }

    // get roles from token
    @Test
    void getRolesFromToken_shouldReturnRole() {
        User user = createUser();
        user.setRole(Role.ADMIN);
        String token = jwtService.generateJwtToken(user);
        assertEquals("ADMIN", jwtService.getRolesFromToken(token));
    }

    @Test
    void getRolesFromToken_shouldReturnNull_whenTokenInvalid() {
        assertNull(jwtService.getRolesFromToken("invalid-token"));
    }

    // get refresh id from refresh token
    @Test
    void getRefreshIdFromRefresh_shouldReturnId() {
        String token = jwtService.generateRefreshToken(createUser(), 10L);
        assertEquals(10L, jwtService.getRefreshIdFromRefresh(token));
    }

    @Test
    void getRefreshIdFromRefresh_shouldReturnNull_forAccessToken() {
        String token = jwtService.generateJwtToken(createUser());
        assertNull(jwtService.getRefreshIdFromRefresh(token));
    }

    @Test
    void getRefreshIdFromRefresh_shouldReturnNull_whenTokenInvalid() {
        assertNull(jwtService.getRefreshIdFromRefresh("invalid-token"));
    }

    // get refresh id allow expired
    @Test
    void getRefreshIdAllowExpired_shouldReturnId_whenTokenExpired() throws Exception {
        setField("jwtRefreshExpiration", 0L);
        jwtService.init();

        String token = jwtService.generateRefreshToken(createUser(), 7L);

        Thread.sleep(10);

        assertFalse(jwtService.validateJwtToken(token));
        assertEquals(7L, jwtService.getRefreshIdAllowExpired(token));
    }

    @Test
    void getRefreshIdAllowExpired_shouldReturnId_whenTokenValid() {
        String token = jwtService.generateRefreshToken(createUser(), 7L);
        assertEquals(7L, jwtService.getRefreshIdAllowExpired(token));
    }

    // get issued at from refresh
    @Test
    void getIssuedAtFromRefresh_shouldReturnIssuedAt_forRefreshToken() {
        String token = jwtService.generateRefreshToken(createUser(), 1L);
        LocalDateTime issuedAt = jwtService.getIssuedAtFromRefresh(token);
        assertNotNull(issuedAt);
        assertTrue(issuedAt.isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void getIssuedAtFromRefresh_shouldReturnNull_forAccessToken() {
        String token = jwtService.generateJwtToken(createUser());
        assertNull(jwtService.getIssuedAtFromRefresh(token));
    }


    // get expiration from refresh
    @Test
    void getExpirationFromRefresh_shouldReturnExpiration() {
        String token = jwtService.generateRefreshToken(createUser(), 10L);
        LocalDateTime expiredAt = jwtService.getExpirationFromRefresh(token);
        assertNotNull(expiredAt);
        assertTrue(expiredAt.isAfter(LocalDateTime.now()));
    }

    @Test
    void getExpirationFromRefresh_shouldReturnNull_whenTokenInvalid() {
        assertNull(jwtService.getExpirationFromRefresh("invalid-token"));
    }

    // test if different roles can generate jwt tokens
    @Test
    void generateJwtToken_shouldWorkWithCoachRole() {
        User user = createUser();
        user.setRole(Role.TRAINER);
        String token = jwtService.generateJwtToken(user);
        assertEquals("TRAINER", jwtService.getRolesFromToken(token));
    }

    @Test
    void generateJwtToken_shouldWorkWithAdminRole() {
        User user = createUser();
        user.setRole(Role.ADMIN);
        String token = jwtService.generateJwtToken(user);
        assertEquals("ADMIN", jwtService.getRolesFromToken(token));
    }

}
