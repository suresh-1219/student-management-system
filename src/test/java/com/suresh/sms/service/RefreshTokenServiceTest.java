package com.suresh.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.suresh.sms.entity.RefreshToken;
import com.suresh.sms.entity.Role;
import com.suresh.sms.entity.User;
import com.suresh.sms.exception.InvalidRefreshTokenException;
import com.suresh.sms.repository.RefreshTokenRepository;

/**
 * Unit tests for the security-critical parts of {@link RefreshTokenService}:
 * that a raw token is never stored, that a used/expired/revoked token is
 * rejected, and that a successful refresh consumes (rotates) the old token.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                refreshTokenService, "refreshTokenExpirationMs", 604_800_000L);

        user = new User();
        user.setId(1L);
        user.setUsername("suresh");
        user.setRole(Role.USER);
    }


    // ISSUE

    @Test
    void issueReturnsARawTokenAndStoresOnlyItsHash() {

        String rawToken = refreshTokenService.issue(user);

        assertNotNull(rawToken);
        assertTrue(rawToken.length() >= 32, "Token should be a long random value");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());

        RefreshToken saved = captor.getValue();

        assertEquals(user, saved.getUser());
        assertNotEquals(rawToken, saved.getTokenHash(),
                "The raw token must never be stored directly");
        assertEquals(64, saved.getTokenHash().length(),
                "A SHA-256 hash, hex-encoded, is always 64 characters");
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void issueProducesADifferentTokenEachTime() {

        String first = refreshTokenService.issue(user);
        String second = refreshTokenService.issue(user);

        assertNotEquals(first, second);
    }


    // VALIDATE AND REVOKE (used by /auth/refresh)

    @Test
    void validateAndRevokeAcceptsAFreshToken() {

        String rawToken = "known-raw-token";

        RefreshToken stored = new RefreshToken(
                user, hashOf(rawToken), LocalDateTime.now().plusDays(1));

        when(repository.findByTokenHash(hashOf(rawToken)))
                .thenReturn(Optional.of(stored));

        User result = refreshTokenService.validateAndRevoke(rawToken);

        assertEquals(user, result);
        assertTrue(stored.isRevoked(), "A used refresh token must be revoked (rotation)");
        verify(repository).save(stored);
    }

    @Test
    void validateAndRevokeRejectsAnUnknownToken() {

        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        InvalidRefreshTokenException ex = assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.validateAndRevoke("nonexistent-token"));

        assertEquals("Invalid or expired refresh token", ex.getMessage());
    }

    @Test
    void validateAndRevokeRejectsAnAlreadyRevokedToken() {

        String rawToken = "reused-token";

        RefreshToken stored = new RefreshToken(
                user, hashOf(rawToken), LocalDateTime.now().plusDays(1));
        stored.setRevoked(true);

        when(repository.findByTokenHash(hashOf(rawToken)))
                .thenReturn(Optional.of(stored));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.validateAndRevoke(rawToken));
    }

    @Test
    void validateAndRevokeRejectsAnExpiredToken() {

        String rawToken = "expired-token";

        RefreshToken stored = new RefreshToken(
                user, hashOf(rawToken), LocalDateTime.now().minusMinutes(1));

        when(repository.findByTokenHash(hashOf(rawToken)))
                .thenReturn(Optional.of(stored));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.validateAndRevoke(rawToken));

        verify(repository, never()).save(any(RefreshToken.class));
    }


    // REVOKE (used by /auth/logout)

    @Test
    void revokeMarksAKnownTokenAsRevoked() {

        String rawToken = "logout-token";

        RefreshToken stored = new RefreshToken(
                user, hashOf(rawToken), LocalDateTime.now().plusDays(1));

        when(repository.findByTokenHash(hashOf(rawToken)))
                .thenReturn(Optional.of(stored));

        refreshTokenService.revoke(rawToken);

        assertTrue(stored.isRevoked());
        verify(repository).save(stored);
    }

    @Test
    void revokeDoesNothingForAnUnknownToken() {

        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        refreshTokenService.revoke("never-issued-token");

        verify(repository, never()).save(any(RefreshToken.class));
    }

    /** Mirrors the private hashing logic in the service, for building fixtures. */
    private static String hashOf(String rawToken) {

        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(
                    rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashed);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
