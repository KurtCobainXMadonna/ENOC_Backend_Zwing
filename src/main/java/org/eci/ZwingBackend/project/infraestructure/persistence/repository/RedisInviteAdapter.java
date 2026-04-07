package org.eci.ZwingBackend.project.infraestructure.persistence.repository;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.project.application.port.out.InviteRepositoryPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@AllArgsConstructor
public class RedisInviteAdapter implements InviteRepositoryPort {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "invite:";

    @Override
    public void saveInvite(String token, UUID projectId, String inviteeEmail, long ttlSeconds) {
        String value = projectId.toString() + "::" + inviteeEmail;
        redisTemplate.opsForValue().set(PREFIX + token, value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<InviteData> findInvite(String token) {
        String value = redisTemplate.opsForValue().get(PREFIX + token);
        if (value == null) return Optional.empty();
        String[] parts = value.split("::");
        if (parts.length != 2) return Optional.empty();

        return Optional.of(new InviteData(UUID.fromString(parts[0]), parts[1]));
    }

    @Override
    public void deleteInvite(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}