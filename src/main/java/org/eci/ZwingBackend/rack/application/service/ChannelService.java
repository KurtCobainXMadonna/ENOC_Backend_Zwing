package org.eci.ZwingBackend.rack.application.service;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.port.in.ChannelLockCase;
import org.eci.ZwingBackend.rack.application.port.in.ManageChannelCase;
import org.eci.ZwingBackend.rack.application.port.out.RackRepositoryPort;
import org.eci.ZwingBackend.rack.application.port.out.SoundLookupPort;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@AllArgsConstructor
public class ChannelService implements ManageChannelCase {
    private final RackRepositoryPort rackRepository;
    private final ChannelLockCase lockCase;
    private final SoundLookupPort soundLookupPort;

    private static final int MAX_CHANNELS_PER_CATEGORY = 2;

    /**
     * In-memory ReentrantLocks keyed by rackId.
     * Serialises concurrent toggleStep calls on the same rack to prevent
     * two threads from reading stale state simultaneously.
     */
    private final ConcurrentHashMap<UUID, ReentrantLock> rackLocks = new ConcurrentHashMap<>();

    private ReentrantLock getRackLock(UUID rackId) {
        return rackLocks.computeIfAbsent(rackId, id -> new ReentrantLock());
    }

    @Override
    @Transactional
    public Channel addChannel(UUID projectId, String name, UUID soundId, UUID requesterId) {
        if (lockCase.isPlaybackLocked(projectId)) {
            throw new RuntimeException("Cannot add channels during playback.");
        }
        if (!soundLookupPort.soundExists(soundId)) {
            throw new RuntimeException("Sound not found: " + soundId);
        }

        ChannelRack rack = rackRepository.getByProjectId(projectId);

        String category = soundLookupPort.getCategoryBySound(soundId);
        long existingCount = rack.getChannels().stream()
                .filter(c -> soundLookupPort.getCategoryBySound(c.getSoundId()).equals(category))
                .count();

        if (existingCount >= MAX_CHANNELS_PER_CATEGORY) {
            throw new RuntimeException(
                    "Maximum " + MAX_CHANNELS_PER_CATEGORY + " channels allowed for category: " + category);
        }

        Channel channel = new Channel(
                UUID.randomUUID(), rack.getRackId(), name, soundId, rack.getChannels().size());

        rack.addChannel(channel);
        rackRepository.save(rack);
        return channel;
    }

    @Override
    @Transactional
    public void removeChannel(UUID projectId, UUID channelId, UUID requesterId) {
        if (lockCase.isPlaybackLocked(projectId)) {
            throw new RuntimeException("Cannot remove channels during playback.");
        }

        String lockHolder = lockCase.getChannelLockHolder(projectId, channelId);
        if (lockHolder != null && !lockHolder.equals(requesterId.toString())) {
            throw new RuntimeException("Channel is being edited by another user.");
        }

        ChannelRack rack = rackRepository.getByProjectId(projectId);
        rack.removeChannel(channelId);
        rackRepository.save(rack);
        lockCase.releaseChannelLock(projectId, channelId, requesterId);
    }

    @Override
    public boolean toggleStep(UUID projectId, UUID channelId, int stepIndex, UUID requesterId) {
        if (lockCase.isPlaybackLocked(projectId)) {
            throw new RuntimeException("Cannot edit grid during playback.");
        }

        // Get rackId first to acquire the right lock
        ChannelRack rack = rackRepository.getByProjectId(projectId);
        ReentrantLock lock = getRackLock(rack.getRackId());

        lock.lock();
        try {
            // Re-fetch inside the lock to get the freshest state
            ChannelRack freshRack = rackRepository.getByProjectId(projectId);
            Channel channel = freshRack.getChannel(channelId);
            boolean newValue = channel.toggleStep(stepIndex);
            // Async save — broadcast fires immediately in the WS controller
            saveRackAsync(freshRack);
            return newValue;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public Channel updateChannel(UUID projectId, UUID channelId, String name,
                                 UUID soundId, float volume, boolean active, UUID requesterId) {
        if (lockCase.isPlaybackLocked(projectId)) {
            throw new RuntimeException("Cannot edit channels during playback.");
        }

        String lockHolder = lockCase.getChannelLockHolder(projectId, channelId);
        if (!requesterId.toString().equals(lockHolder)) {
            throw new RuntimeException("You do not hold the lock on this channel.");
        }

        ChannelRack rack = rackRepository.getByProjectId(projectId);
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

        rackRepository.save(rack);
        return channel;
    }

    @Async
    public void saveRackAsync(ChannelRack rack) {
        rackRepository.save(rack);
    }
}
