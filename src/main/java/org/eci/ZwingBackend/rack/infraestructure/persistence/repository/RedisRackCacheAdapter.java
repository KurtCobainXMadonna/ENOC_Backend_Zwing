package org.eci.ZwingBackend.rack.infraestructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.rack.application.port.out.RackCachePort;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class RedisRackCacheAdapter implements RackCachePort {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "rack_state:";
    private static final String DIRTY_PREFIX = "rack_dirty:";

    @Override
    public void cacheRack(UUID projectId, ChannelRack rack) {
        try {
            String json = objectMapper.writeValueAsString(rack);
            redisTemplate.opsForValue().set(CACHE_PREFIX + projectId, json);
        } catch (JsonProcessingException e) {
            log.error("[RackCache] Failed to serialize rack for project {}: {}", projectId, e.getMessage());
            throw new RuntimeException("Failed to cache rack state", e);
        }
    }

    @Override
    public Optional<ChannelRack> getCachedRack(UUID projectId) {
        String json = redisTemplate.opsForValue().get(CACHE_PREFIX + projectId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, ChannelRack.class));
        } catch (JsonProcessingException e) {
            log.error("[RackCache] Failed to deserialize rack for project {}: {}", projectId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void evictRack(UUID projectId) {
        redisTemplate.delete(CACHE_PREFIX + projectId);
        redisTemplate.delete(DIRTY_PREFIX + projectId);
    }

    @Override
    public boolean isCached(UUID projectId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CACHE_PREFIX + projectId));
    }

    @Override
    public Set<UUID> getCachedProjectIds() {
        Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }
        return keys.stream()
                .map(key -> key.substring(CACHE_PREFIX.length()))
                .map(this::parseUuidSafely)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public void markDirty(UUID projectId) {
        redisTemplate.opsForValue().set(DIRTY_PREFIX + projectId, "1");
    }

    @Override
    public boolean isDirty(UUID projectId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(DIRTY_PREFIX + projectId));
    }

    @Override
    public void clearDirty(UUID projectId) {
        redisTemplate.delete(DIRTY_PREFIX + projectId);
    }

    private UUID parseUuidSafely(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            log.warn("[RackCache] Skipping non-UUID cache key suffix: {}", raw);
            return null;
        }
    }
}
