package org.eci.ZwingBackend.presence.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.presence.application.port.out.PresenceStorePort;
import org.eci.ZwingBackend.presence.domain.model.Presence;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class RedisPresenceAdapter implements PresenceStorePort {
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper mapper;

    public RedisPresenceAdapter(@Qualifier("redisTemplate") RedisTemplate<String, String> redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    private String presenceKey(UUID projectId) {
        return "presence:project:" + projectId;
    }

    private String colorsKey(UUID projectId) {
        return "presence:colors:" + projectId;
    }

    @Override
    public void savePresence(Presence presence) {
        try {
            String json = mapper.writeValueAsString(presence);
            redis.opsForHash().put(presenceKey(presence.getProjectId()),
                    presence.getUserId(), json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Presence", e);
        }
    }

    @Override
    public void removePresence(UUID projectId, String userId) {
        redis.opsForHash().delete(presenceKey(projectId), userId);
    }

    @Override
    public Optional<Presence> findPresence(UUID projectId, String userId) {
        Object raw = redis.opsForHash().get(presenceKey(projectId), userId);
        if (raw == null) return Optional.empty();
        return Optional.ofNullable(deserialize(raw.toString()));
    }

    @Override
    public List<Presence> findAllInProject(UUID projectId) {
        Map<Object, Object> entries = redis.opsForHash().entries(presenceKey(projectId));
        List<Presence> result = new ArrayList<>(entries.size());
        for (Object value : entries.values()) {
            Presence p = deserialize(value.toString());
            if (p != null) result.add(p);
        }
        return result;
    }

    @Override
    public long countInProject(UUID projectId) {
        Long size = redis.opsForHash().size(presenceKey(projectId));
        return size == null ? 0L : size;
    }

    private static final String CLAIM_COLOR_LUA = """
            for i = 1, #ARGV do
                if redis.call('SADD', KEYS[1], ARGV[i]) == 1 then
                    return ARGV[i]
                end
            end
            return nil
            """;

    @Override
    public Optional<String> claimColor(UUID projectId, List<String> palette) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>(CLAIM_COLOR_LUA, String.class);
        String claimed = redis.execute(script, List.of(colorsKey(projectId)), palette.toArray());
        return Optional.ofNullable(claimed);
    }

    @Override
    public void releaseColor(UUID projectId, String colorHex) {
        redis.opsForSet().remove(colorsKey(projectId), colorHex);
    }

    @Override
    public void clearProject(UUID projectId) {
        redis.delete(presenceKey(projectId));
        redis.delete(colorsKey(projectId));
    }

    private Presence deserialize(String json) {
        try {
            return mapper.readValue(json, Presence.class);
        } catch (JsonProcessingException e) {
            log.error("[Presence] Failed to deserialize Presence JSON: {}", json, e);
            return null;
        }
    }
}
