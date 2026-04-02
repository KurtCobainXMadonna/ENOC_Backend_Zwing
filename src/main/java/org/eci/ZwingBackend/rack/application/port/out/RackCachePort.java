package org.eci.ZwingBackend.rack.application.port.out;

import org.eci.ZwingBackend.rack.domain.model.ChannelRack;

import java.util.Optional;
import java.util.UUID;

public interface RackCachePort {
    /** Save the full rack state to Redis. */
    void cacheRack(UUID projectId, ChannelRack rack);

    /** Retrieve the cached rack state. Returns empty if no cache entry exists. */
    Optional<ChannelRack> getCachedRack(UUID projectId);

    /** Remove the cache entry (called after flushing to PostgreSQL). */
    void evictRack(UUID projectId);

    /** Check whether a cached entry exists for this project. */
    boolean isCached(UUID projectId);
}
