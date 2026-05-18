package org.eci.ZwingBackend.auth.application.service;

import org.eci.ZwingBackend.auth.application.port.in.RefreshSessionUseCase;
import org.eci.ZwingBackend.auth.application.port.out.RefreshTokenStorePort;
import org.eci.ZwingBackend.auth.application.port.out.TokenGeneratorPort;
import org.eci.ZwingBackend.auth.application.port.out.UserRepositoryAuthOutPort;
import org.eci.ZwingBackend.auth.domain.model.RefreshTokenData;
import org.eci.ZwingBackend.auth.domain.model.TokenPair;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService implements RefreshSessionUseCase {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RAW_TOKEN_BYTES = 32;

    private final RefreshTokenStorePort store;
    private final TokenGeneratorPort accessTokenGenerator;
    private final UserRepositoryAuthOutPort userRepository;
    private final long refreshTtlSeconds;

    public RefreshTokenService(RefreshTokenStorePort store, TokenGeneratorPort accessTokenGenerator, UserRepositoryAuthOutPort userRepository, @Value("${auth.refresh-token.ttl-seconds:3600}") long refreshTtlSeconds) {
        this.store = store;
        this.accessTokenGenerator = accessTokenGenerator;
        this.userRepository = userRepository;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String issue(User user) {
        String raw = generateRawToken();
        store.save(hash(raw), new RefreshTokenData(user.getUserId(), user.getEmail()), refreshTtlSeconds);
        return raw;
    }

    public long ttlSeconds() {
        return refreshTtlSeconds;
    }

    @Override
    public TokenPair refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Missing refresh token");
        }

        String oldHash = hash(rawRefreshToken);
        RefreshTokenData data = store.find(oldHash).orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));

        store.delete(oldHash);

        User user = userRepository.findById(data.userId()).orElseThrow(() -> new InvalidRefreshTokenException("User no longer exists"));
        String newAccess = accessTokenGenerator.generateToken(user);
        String newRaw = generateRawToken();
        store.save(hash(newRaw), new RefreshTokenData(user.getUserId(), user.getEmail()), refreshTtlSeconds);

        return new TokenPair(newAccess, newRaw);
    }

    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        store.delete(hash(rawRefreshToken));
    }

    public void revokeAllForUser(UUID userId) {
        store.deleteAllForUser(userId);
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) { super(message); }
    }
}
