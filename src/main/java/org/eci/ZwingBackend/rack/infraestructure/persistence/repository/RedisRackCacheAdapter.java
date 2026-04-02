package org.eci.ZwingBackend.rack.infraestructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.rack.application.port.out.RackCachePort;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class RedisRackCacheAdapter implements RackCachePort {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "rack_state:";

    @Override
    public void cacheRack(UUID projectId, ChannelRack rack) {
        try {
            String json = objectMapper.writeValueAsString(rack);
            redisTemplate.opsForValue().set(PREFIX + projectId, json);
        } catch (JsonProcessingException e) {
            log.error("[RackCache] Failed to serialize rack for project {}: {}", projectId, e.getMessage());
            throw new RuntimeException("Failed to cache rack state", e);
        }
    }

    @Override
    public Optional<ChannelRack> getCachedRack(UUID projectId) {
        String json = redisTemplate.opsForValue().get(PREFIX + projectId);
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
        redisTemplate.delete(PREFIX + projectId);
    }

    @Override
    public boolean isCached(UUID projectId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + projectId));
    }
}
