package org.eci.ZwingBackend.auth.application.port.out;

import org.eci.ZwingBackend.auth.domain.model.RefreshTokenData;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStorePort {
    void save(String tokenHash, RefreshTokenData data, long ttlSeconds);
    Optional<RefreshTokenData> find(String tokenHash);
    void delete(String tokenHash);
    void deleteAllForUser(UUID userId);
}
