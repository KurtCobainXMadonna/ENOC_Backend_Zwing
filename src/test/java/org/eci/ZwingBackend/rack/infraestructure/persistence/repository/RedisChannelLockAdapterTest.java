package org.eci.ZwingBackend.rack.infraestructure.persistence.repository;

import org.eci.ZwingBackend.rack.application.port.out.ChannelLockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisChannelLockAdapterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ChannelLockPort adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisChannelLockAdapter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void acquireLockUsesSetIfAbsentWithTtl() {
        when(valueOperations.setIfAbsent("lock", "user", Duration.ofSeconds(30))).thenReturn(Boolean.TRUE);

        assertThat(adapter.acquireLock("lock", "user", 30)).isTrue();
        verify(valueOperations).setIfAbsent("lock", "user", Duration.ofSeconds(30));
    }

    @Test
    void acquireLockReturnsFalseWhenLockCannotBeGranted() {
        when(valueOperations.setIfAbsent("lock", "user", Duration.ofSeconds(30))).thenReturn(Boolean.FALSE);

        assertThat(adapter.acquireLock("lock", "user", 30)).isFalse();
    }

    @Test
    void releaseAndReadAndWriteOperationsDelegateToRedis() {
        when(valueOperations.get("lock")).thenReturn("user-1");
        when(redisTemplate.hasKey("lock")).thenReturn(Boolean.TRUE, Boolean.FALSE);

        adapter.releaseLock("lock", "user-1");
        adapter.setLock("lock", "user-1");
        adapter.deleteLock("lock");

        assertThat(adapter.getLockHolder("lock")).isEqualTo("user-1");
        assertThat(adapter.lockExists("lock")).isTrue();
        assertThat(adapter.lockExists("lock")).isFalse();

        verify(redisTemplate).execute(any(), eq(List.of("lock")), eq("user-1"));
        verify(valueOperations).set("lock", "user-1");
        verify(redisTemplate).delete("lock");
    }
}