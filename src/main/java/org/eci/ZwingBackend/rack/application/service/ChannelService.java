package org.eci.ZwingBackend.rack.application.service;

import org.eci.ZwingBackend.rack.application.port.in.ChannelLockCase;
import org.eci.ZwingBackend.rack.application.port.in.ManageChannelCase;
import org.eci.ZwingBackend.rack.application.port.out.RackCachePort;
import org.eci.ZwingBackend.rack.application.port.out.RackRepositoryPort;
import org.eci.ZwingBackend.rack.application.port.out.SoundLookupPort;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ChannelService implements ManageChannelCase {
    private final RackRepositoryPort rackRepository;
    private final RackCachePort rackCache;
    private final ChannelLockCase lockCase;
    private final SoundLookupPort soundLookupPort;

    private static final int MAX_CHANNELS_PER_CATEGORY = 2;

    private final ConcurrentHashMap<UUID, ReentrantLock> projectLocks = new ConcurrentHashMap<>();

    public ChannelService(RackRepositoryPort rackRepository, RackCachePort rackCache, ChannelLockCase lockCase, SoundLookupPort soundLookupPort) {
        this.rackRepository = rackRepository;
        this.rackCache = rackCache;
        this.lockCase = lockCase;
        this.soundLookupPort = soundLookupPort;
    }

    private ReentrantLock getProjectLock(UUID projectId) {
        return projectLocks.computeIfAbsent(projectId, id -> new ReentrantLock());
    }

    private ChannelRack getLiveRack(UUID projectId) {
        return rackCache.getCachedRack(projectId).orElseGet(() -> {
            ChannelRack fromDb = rackRepository.getByProjectId(projectId);
            rackCache.cacheRack(projectId, fromDb);
            return fromDb;
        });
    }

    private void saveLiveRack(UUID projectId, ChannelRack rack) {
        rackCache.cacheRack(projectId, rack);
    }

    // ── mutations ─────────────────────────────────────────────────────────────

    @Override
    public Channel addChannel(UUID projectId, String name, UUID soundId, UUID requesterId) {
        if (lockCase.isPlaybackLocked(projectId)) {
            throw new RuntimeException("Cannot add channels during playback.");
        }
        if (!soundLookupPort.soundExists(soundId)) {
            throw new RuntimeException("Sound not found: " + soundId);
        }

        ReentrantLock lock = getProjectLock(projectId);
        lock.lock();
        try {
            ChannelRack rack = getLiveRack(projectId);

            String category = soundLookupPort.getCategoryBySound(soundId);
            long existingCount = rack.getChannels().stream()
                    .filter(c -> soundLookupPort.getCategoryBySound(c.getSoundId()).equals(category))
                    .count();

            if (existingCount >= MAX_CHANNELS_PER_CATEGORY) {
                throw new RuntimeException(
                        "Maximum " + MAX_CHANNELS_PER_CATEGORY + " channels allowed for category: " + category);
            }

            Channel channel = new Channel(UUID.randomUUID(), rack.getRackId(), name, soundId, rack.getChannels().size());
            rack.addChannel(channel);
            saveLiveRack(projectId, rack);
            return channel;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeChannel(UUID projectId, UUID channelId, UUID requesterId) {
        if (lockCase.isPlaybackLocked(projectId)) {
            throw new RuntimeException("Cannot remove channels during playback.");
        }

        String lockHolder = lockCase.getChannelLockHolder(projectId, channelId);
        if (lockHolder != null && !lockHolder.equals(requesterId.toString())) {
            throw new RuntimeException("Channel is being edited by another user.");
        }
        ReentrantLock lock = getProjectLock(projectId);
        lock.lock();
        try {
            ChannelRack rack = getLiveRack(projectId);
            rack.removeChannel(channelId);
            saveLiveRack(projectId, rack);
            lockCase.releaseChannelLock(projectId, channelId, requesterId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean toggleStep(UUID projectId, UUID channelId, int stepIndex, UUID requesterId) {
        if (lockCase.isPlaybackLocked(projectId)) {
            throw new RuntimeException("Cannot edit grid during playback.");
        }

        ReentrantLock lock = getProjectLock(projectId);
        lock.lock();
        try {
            ChannelRack rack = getLiveRack(projectId);
            Channel channel = rack.getChannel(channelId);
            boolean newValue = channel.toggleStep(stepIndex);
            saveLiveRack(projectId, rack);          // Redis only — fast, no DB roundtrip
            return newValue;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Channel updateChannel(UUID projectId, UUID channelId, String name,
                                 UUID soundId, float volume, boolean active, UUID requesterId) {
        if (lockCase.isPlaybackLocked(projectId)) {
            throw new RuntimeException("Cannot edit channels during playback.");
        }

        String lockHolder = lockCase.getChannelLockHolder(projectId, channelId);
        if (!requesterId.toString().equals(lockHolder)) {
            throw new RuntimeException("You do not hold the lock on this channel.");
        }

        ReentrantLock lock = getProjectLock(projectId);
        lock.lock();
        try {
            ChannelRack rack = getLiveRack(projectId);
            Channel channel = rack.getChannel(channelId);

            if (name != null) channel.setName(name);
            if (soundId != null) {
                if (!soundLookupPort.soundExists(soundId)) {
                    throw new RuntimeException("Sound not found: " + soundId);
                }
                channel.setSoundId(soundId);
            }
            channel.setVolume(Math.max(0f, Math.min(1f, volume)));
            channel.setActive(active);

            saveLiveRack(projectId, rack);
            return channel;
        } finally {
            lock.unlock();
        }
    }
}
