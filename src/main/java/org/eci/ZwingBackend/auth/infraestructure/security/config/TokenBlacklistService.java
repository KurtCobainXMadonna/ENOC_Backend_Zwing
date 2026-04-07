package org.eci.ZwingBackend.auth.infraestructure.security.config;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.auth.application.port.out.TokenBlacklistPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class TokenBlacklistService implements TokenBlacklistPort {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void blacklistToken(String token, long durationSeconds) {
        redisTemplate.opsForValue().set("blacklisted_token:" + token, "true", durationSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklisted_token:" + token));
    }

    public void blacklistUser(String userId, long durationSeconds) {
        redisTemplate.opsForValue().set("blacklisted_user:" + userId, "true", durationSeconds, TimeUnit.SECONDS);
    }

    public boolean isUserBlacklisted(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklisted_user:" + userId));
    }
}