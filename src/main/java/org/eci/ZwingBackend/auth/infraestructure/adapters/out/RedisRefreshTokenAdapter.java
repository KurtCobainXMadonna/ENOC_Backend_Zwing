package org.eci.ZwingBackend.auth.infraestructure.adapters.out;

import org.eci.ZwingBackend.auth.application.port.out.RefreshTokenStorePort;
import org.eci.ZwingBackend.auth.domain.model.RefreshTokenData;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisRefreshTokenAdapter implements RefreshTokenStorePort {
    private static final String TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String USER_INDEX_PREFIX = "user_refresh_tokens:";
    private static final String VALUE_SEPARATOR = "|";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisRefreshTokenAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String tokenHash, RefreshTokenData data, long ttlSeconds) {
        String tokenKey = TOKEN_KEY_PREFIX + tokenHash;
        String userKey = USER_INDEX_PREFIX + data.userId();
        String payload = data.userId() + VALUE_SEPARATOR + data.email();

        redisTemplate.opsForValue().set(tokenKey, payload, ttlSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(userKey, tokenHash);
        redisTemplate.expire(userKey, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<RefreshTokenData> find(String tokenHash) {
        String payload = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + tokenHash);
        if (payload == null) return Optional.empty();

        int sep = payload.indexOf(VALUE_SEPARATOR);
        if (sep < 0) return Optional.empty();

        UUID userId = UUID.fromString(payload.substring(0, sep));
        String email = payload.substring(sep + 1);
        return Optional.of(new RefreshTokenData(userId, email));
    }

    @Override
    public void delete(String tokenHash) {
        String tokenKey = TOKEN_KEY_PREFIX + tokenHash;
        String payload = redisTemplate.opsForValue().get(tokenKey);
        redisTemplate.delete(tokenKey);

        if (payload != null) {
            int sep = payload.indexOf(VALUE_SEPARATOR);
            if (sep > 0) {
                String userId = payload.substring(0, sep);
                redisTemplate.opsForSet().remove(USER_INDEX_PREFIX + userId, tokenHash);
            }
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        String userKey = USER_INDEX_PREFIX + userId;
        Set<String> hashes = redisTemplate.opsForSet().members(userKey);
        if (hashes != null) {
            for (String hash : hashes) {
                redisTemplate.delete(TOKEN_KEY_PREFIX + hash);
            }
        }
        redisTemplate.delete(userKey);
    }
}
