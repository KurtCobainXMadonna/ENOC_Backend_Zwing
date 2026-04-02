package org.eci.ZwingBackend.rack.application.port.out;

public interface ChannelLockPort {
    /** Atomically sets the lock only if it doesn't exist (Redis SET NX). Returns true if granted. */
    boolean acquireLock(String lockKey, String userId, long ttlSeconds);
    /** Releases the lock only if the caller is the current holder (Lua script, atomic). */
    void releaseLock(String lockKey, String userId);
    /** Returns the userId currently holding the lock, or null if no lock exists. */
    String getLockHolder(String lockKey);
    /** Sets a value with no TTL — used for the playback lock. */
    void setLock(String lockKey, String userId);
    void deleteLock(String lockKey);
    boolean lockExists(String lockKey);
}
