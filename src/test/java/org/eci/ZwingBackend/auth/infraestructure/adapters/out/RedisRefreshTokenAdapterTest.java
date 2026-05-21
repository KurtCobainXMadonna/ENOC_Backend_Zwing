package org.eci.ZwingBackend.auth.infraestructure.adapters.out;

import org.eci.ZwingBackend.auth.domain.model.RefreshTokenData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenAdapterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private RedisRefreshTokenAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisRefreshTokenAdapter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void saveFindDeleteAndDeleteAllForUserWork() {
        UUID userId = UUID.randomUUID();
        RefreshTokenData data = new RefreshTokenData(userId, "user@example.com");

        adapter.save("hash", data, 3600);
        when(valueOperations.get("refresh_token:hash")).thenReturn(userId + "|user@example.com");

        Optional<RefreshTokenData> found = adapter.find("hash");

        adapter.delete("hash");
        when(setOperations.members("user_refresh_tokens:" + userId)).thenReturn(Set.of("hash1", "hash2"));
        adapter.deleteAllForUser(userId);

        assertThat(found).contains(data);
        verify(valueOperations).set("refresh_token:hash", userId + "|user@example.com", 3600, java.util.concurrent.TimeUnit.SECONDS);
        verify(setOperations).add("user_refresh_tokens:" + userId, "hash");
        verify(redisTemplate).delete("refresh_token:hash");
        verify(setOperations).remove("user_refresh_tokens:" + userId, "hash");
        verify(redisTemplate).delete("user_refresh_tokens:" + userId);
    }
}