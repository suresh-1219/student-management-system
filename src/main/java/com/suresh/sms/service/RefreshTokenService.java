package com.suresh.sms.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suresh.sms.entity.RefreshToken;
import com.suresh.sms.entity.User;
import com.suresh.sms.exception.InvalidRefreshTokenException;
import com.suresh.sms.repository.RefreshTokenRepository;

/**
 * Issues, rotates and revokes refresh tokens.
 *
 * <p>Only a SHA-256 hash of each token is ever stored, so a database leak
 * can't be used to impersonate a user. The raw token itself is a
 * cryptographically random 256-bit value returned to the client exactly
 * once, at issue time.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    @Autowired
    private RefreshTokenRepository repository;

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(User user) {

        String rawToken = generateRawToken();

        RefreshToken entity = new RefreshToken(
                user,
                hash(rawToken),
                LocalDateTime.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS));

        repository.save(entity);

        return rawToken;
    }

    /**
     * Validates a refresh token and immediately revokes it (rotation): every
     * successful refresh consumes the old token and the caller must issue a
     * new one via {@link #issue}. This limits how long a stolen refresh
     * token remains useful.
     */
    @Transactional
    public User validateAndRevoke(String rawToken) {

        RefreshToken entity = repository.findByTokenHash(hash(rawToken))
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        "Invalid or expired refresh token"));

        entity.setRevoked(true);
        repository.save(entity);

        return entity.getUser();
    }

    /** Revokes a refresh token (logout). Silently does nothing if it is unknown already. */
    public void revoke(String rawToken) {

        repository.findByTokenHash(hash(rawToken))
                .ifPresent(t -> {
                    t.setRevoked(true);
                    repository.save(t);
                });
    }

    private String generateRawToken() {

        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashed);

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on every JVM.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
