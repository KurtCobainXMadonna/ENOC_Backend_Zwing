package org.eci.ZwingBackend.rack.application.port.in;

import java.util.UUID;

public interface ChannelLockCase {
    /** Tries to acquire a lock on a channel. Returns true if granted, false if someone else holds it. */
    boolean acquireChannelLock(UUID projectId, UUID channelId, UUID userId);
    /** Releases the channel lock. Only the holder can release their own lock. */
    void releaseChannelLock(UUID projectId, UUID channelId, UUID userId);
    /** Returns the userId of the current lock holder, or null if unlocked. */
    String getChannelLockHolder(UUID projectId, UUID channelId);
    /** Acquires the project-wide playback lock — blocks all editing. */
    void acquirePlaybackLock(UUID projectId, UUID userId);
    /** Releases the playback lock — editing resumes for all users. */
    void releasePlaybackLock(UUID projectId);
    boolean isPlaybackLocked(UUID projectId);
}
