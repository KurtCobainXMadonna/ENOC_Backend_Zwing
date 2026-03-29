package org.eci.ZwingBackend.auth.infraestructure.security.config;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class TokenBlacklistService {
    private final RedisTemplate<String, String> redisTemplate;

    public void blacklistUser(String userId, long durationSeconds) {
        redisTemplate.opsForValue().set("blacklisted_user:" + userId, "true", durationSeconds, TimeUnit.SECONDS);
    }

    public boolean isUserBlacklisted(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklisted_user:" + userId));
    }
}