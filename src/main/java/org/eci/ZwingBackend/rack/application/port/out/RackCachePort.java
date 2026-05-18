package org.eci.ZwingBackend.rack.application.port.out;

import org.eci.ZwingBackend.rack.domain.model.ChannelRack;

import java.util.Optional;
import java.util.Set;
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

    /** Return the project IDs of every rack currently in cache. */
    Set<UUID> getCachedProjectIds();

    /** Mark a project rack as having unflushed changes since the last periodic flush. */
    void markDirty(UUID projectId);

    /** Whether the project has unflushed mutations awaiting the next periodic flush. */
    boolean isDirty(UUID projectId);

    /** Clear the dirty flag (called after a successful flush). */
    void clearDirty(UUID projectId);
}
