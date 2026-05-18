package org.eci.ZwingBackend.rack.application.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.rack.application.port.out.RackCachePort;
import org.eci.ZwingBackend.rack.application.port.out.RackRepositoryPort;
import org.eci.ZwingBackend.rack.application.port.out.ChannelLockPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class RackSessionService {
    private final RackCachePort rackCache;
    private final RackRepositoryPort rackRepository;
    private final ChannelLockPort lockPort;

    /**
     * Flush the Redis rack state to PostgreSQL and evict the cache.
     * Called when the last user leaves the project room.
     */
    @Transactional
    public void flushToDatabase(UUID projectId) {
        rackCache.getCachedRack(projectId).ifPresent(rack -> {
            rackRepository.save(rack);
            rackCache.evictRack(projectId);
            log.info("[RackSession] Flushed rack state to PostgreSQL for project {} and evicted cache.", projectId);
        });
    }

    /**
     * Flush every cached rack with unflushed mutations to PostgreSQL.
     * The cache is NOT evicted — only the on-last-leave path owns eviction,
     * since active sessions still rely on Redis as the source of truth.
     * Failures on a single project do not abort the others.
     */
    public void flushAllDirty() {
        Set<UUID> projectIds = rackCache.getCachedProjectIds();
        if (projectIds.isEmpty()) {
            return;
        }

        int flushed = 0;
        for (UUID projectId : projectIds) {
            if (!rackCache.isDirty(projectId)) {
                continue;
            }
            try {
                rackCache.getCachedRack(projectId).ifPresent(rack -> {
                    rackRepository.save(rack);
                    rackCache.clearDirty(projectId);
                    log.info("[RackSession] Periodic flush wrote project {} to PostgreSQL.", projectId);
                });
                flushed++;
            } catch (Exception e) {
                log.error("[RackSession] Periodic flush failed for project {}: {}", projectId, e.getMessage(), e);
            }
        }

        if (flushed > 0) {
            log.info("[RackSession] Periodic flush cycle complete — {} dirty project(s) processed.", flushed);
        }
    }

    /**
     * Release the playback lock if this user held it.
     * The playback lock key has no TTL, so it must be cleaned up on disconnect.
     */
    public void releasePlaybackLockIfHolder(UUID projectId, String userId) {
        String playbackKey = "playback_lock:" + projectId;
        String holder = lockPort.getLockHolder(playbackKey);
        if (userId.equals(holder)) {
            lockPort.deleteLock(playbackKey);
            log.info("[RackSession] Released playback lock for project {} (user {} disconnected).", projectId, userId);
        }
    }

    /**
     * Release all channel locks held by a specific user in a project.
     * Channel locks have TTL so they'd auto-expire, but explicit release
     * gives faster recovery and cleaner UX for other collaborators.
     */
    public void releaseAllChannelLocks(UUID projectId, String userId) {
        // Channel locks are keyed as channel_lock:{projectId}:{channelId}
        // We need to find all locks for this project held by this user.
        // Since we don't track which channels a user has locked, we rely on
        // the rack state to enumerate channels and check each one.
        rackCache.getCachedRack(projectId).ifPresent(rack ->
                rack.getChannels().forEach(channel -> {
                    String lockKey = "channel_lock:" + projectId + ":" + channel.getChannelId();
                    String holder = lockPort.getLockHolder(lockKey);
                    if (userId.equals(holder)) {
                        lockPort.releaseLock(lockKey, userId);
                        log.info("[RackSession] Released channel lock {} for user {}.", channel.getChannelId(), userId);
                    }
                })
        );
    }
}
