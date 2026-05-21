package org.eci.ZwingBackend.presence.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eci.ZwingBackend.presence.domain.model.Presence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisPresenceAdapterTest {

    @Mock
    private RedisTemplate<String, String> redis;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        when(redis.opsForHash()).thenReturn(hashOperations);
        when(redis.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void saveFindListCountClaimAndClearPresenceWork() throws Exception {
        UUID projectId = UUID.randomUUID();
        String userId = "user-1";
        Presence presence = new Presence(projectId, userId, "user@example.com", "User", "#123456", Instant.parse("2026-05-21T12:00:00Z"));
        ObjectMapper realMapper = new ObjectMapper().findAndRegisterModules();
        RedisPresenceAdapter adapter = new RedisPresenceAdapter(redis, realMapper);
        String json = realMapper.writeValueAsString(presence);

        when(hashOperations.get("presence:project:" + projectId, userId)).thenReturn(json);
        when(hashOperations.entries("presence:project:" + projectId)).thenReturn(new HashMap<>(Map.of(userId, json)));
        when(hashOperations.size("presence:project:" + projectId)).thenReturn(1L);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class))).thenReturn("#abcdef");

        adapter.savePresence(presence);
        assertThat(adapter.findPresence(projectId, userId)).contains(presence);
        assertThat(adapter.findAllInProject(projectId)).containsExactly(presence);
        assertThat(adapter.countInProject(projectId)).isEqualTo(1L);
        assertThat(adapter.claimColor(projectId, List.of("#abcdef", "#123456"))).contains("#abcdef");

        adapter.removePresence(projectId, userId);
        adapter.releaseColor(projectId, "#abcdef");
        adapter.clearProject(projectId);

        verify(hashOperations).put("presence:project:" + projectId, userId, json);
        verify(hashOperations).delete("presence:project:" + projectId, userId);
        verify(setOperations).remove("presence:colors:" + projectId, "#abcdef");
        verify(redis).delete("presence:project:" + projectId);
        verify(redis).delete("presence:colors:" + projectId);
    }

    @Test
    void deserializationAndSerializationErrorsAreHandled() throws Exception {
        UUID projectId = UUID.randomUUID();
        String userId = "user-2";
        Presence presence = new Presence(projectId, userId, "user2@example.com", "User 2", "#654321", Instant.now());
        RedisPresenceAdapter failingAdapter = new RedisPresenceAdapter(redis, mapper);
        RedisPresenceAdapter realAdapter = new RedisPresenceAdapter(redis, new ObjectMapper());

        when(mapper.writeValueAsString(presence)).thenThrow(new JsonProcessingException("boom") {});
        assertThatThrownBy(() -> failingAdapter.savePresence(presence))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize Presence");

        when(hashOperations.get("presence:project:" + projectId, userId)).thenReturn("broken");
        when(hashOperations.entries("presence:project:" + projectId)).thenReturn(Map.of(userId, "broken"));
        when(hashOperations.size("presence:project:" + projectId)).thenReturn(null);

        assertThat(realAdapter.findPresence(projectId, userId)).isEmpty();
        assertThat(realAdapter.findAllInProject(projectId)).isEmpty();
        assertThat(realAdapter.countInProject(projectId)).isZero();
    }
}