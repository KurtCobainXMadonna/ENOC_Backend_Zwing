package org.eci.ZwingBackend.rack.infraestructure.persistence.repository;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.port.out.ChannelLockPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@AllArgsConstructor
public class RedisChannelLockAdapter implements ChannelLockPort {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean acquireLock(String lockKey, String userId, long ttlSeconds) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, userId, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseLock(String lockKey, String userId) {
        String lua = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(lua, Long.class);
        redisTemplate.execute(script, List.of(lockKey), userId);
    }

    @Override
    public String getLockHolder(String lockKey) {
        return redisTemplate.opsForValue().get(lockKey);
    }

    @Override
    public void setLock(String lockKey, String userId) {
        redisTemplate.opsForValue().set(lockKey, userId);
    }

    @Override
    public void deleteLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }

    @Override
    public boolean lockExists(String lockKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
}
