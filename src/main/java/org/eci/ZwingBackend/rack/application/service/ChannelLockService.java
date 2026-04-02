package org.eci.ZwingBackend.rack.application.service;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.port.in.ChannelLockCase;
import org.eci.ZwingBackend.rack.application.port.out.ChannelLockPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Centralises all lock logic. Two lock types:
 *
 * 1. CHANNEL LOCK — per-channel, 30s TTL (auto-expires on crash/disconnect)
 *    Key: channel_lock:{projectId}:{channelId}  →  userId
 *
 * 2. PLAYBACK LOCK — project-wide, 5-minute safety TTL
 *    Key: playback_lock:{projectId}  →  userId
 *    Uses SET NX so two concurrent play requests can't both succeed.
 *    The 5-minute TTL is a safety net — normal cleanup happens via
 *    the disconnect handler or explicit stop.
 *
 * Redis SET NX guarantees atomicity — two concurrent requests
 * cannot both think they acquired the same lock.
 */
@Service
@AllArgsConstructor
public class ChannelLockService implements ChannelLockCase {

    private final ChannelLockPort lockPort;

    private static final long CHANNEL_LOCK_TTL_SECONDS = 30;
    private static final long PLAYBACK_LOCK_TTL_SECONDS = 300; // 5-minute safety net

    private String channelLockKey(UUID projectId, UUID channelId) {
        return "channel_lock:" + projectId + ":" + channelId;
    }

    private String playbackLockKey(UUID projectId) {
        return "playback_lock:" + projectId;
    }

    @Override
    public boolean acquireChannelLock(UUID projectId, UUID channelId, UUID userId) {
        return lockPort.acquireLock(
                channelLockKey(projectId, channelId), userId.toString(), CHANNEL_LOCK_TTL_SECONDS);
    }

    @Override
    public void releaseChannelLock(UUID projectId, UUID channelId, UUID userId) {
        lockPort.releaseLock(channelLockKey(projectId, channelId), userId.toString());
    }

    @Override
    public String getChannelLockHolder(UUID projectId, UUID channelId) {
        return lockPort.getLockHolder(channelLockKey(projectId, channelId));
    }

    @Override
    public void acquirePlaybackLock(UUID projectId, UUID userId) {
        boolean acquired = lockPort.acquireLock(
                playbackLockKey(projectId), userId.toString(), PLAYBACK_LOCK_TTL_SECONDS);
        if (!acquired) {
            throw new RuntimeException("Playback is already active.");
        }
    }

    @Override
    public void releasePlaybackLock(UUID projectId) {
        lockPort.deleteLock(playbackLockKey(projectId));
    }

    @Override
    public boolean isPlaybackLocked(UUID projectId) {
        return lockPort.lockExists(playbackLockKey(projectId));
    }
}
