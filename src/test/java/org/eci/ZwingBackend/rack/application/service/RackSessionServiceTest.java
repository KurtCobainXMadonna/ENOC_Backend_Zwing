package org.eci.ZwingBackend.rack.application.service;

import org.eci.ZwingBackend.rack.application.port.out.ChannelLockPort;
import org.eci.ZwingBackend.rack.application.port.out.RackCachePort;
import org.eci.ZwingBackend.rack.application.port.out.RackRepositoryPort;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RackSessionServiceTest {

    @Mock
    private RackCachePort rackCache;

    @Mock
    private RackRepositoryPort rackRepository;

    @Mock
    private ChannelLockPort lockPort;

    private RackSessionService rackSessionService;

    @BeforeEach
    void setUp() {
        rackSessionService = new RackSessionService(rackCache, rackRepository, lockPort);
    }

    @Test
    void flushToDatabaseWritesRackAndEvictsCacheWhenPresent() {
        UUID projectId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);

        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.of(rack));

        rackSessionService.flushToDatabase(projectId);

        verify(rackRepository).save(rack);
        verify(rackCache).evictRack(projectId);
    }

    @Test
    void flushToDatabaseSkipsMissingCacheEntry() {
        UUID projectId = UUID.randomUUID();

        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.empty());

        rackSessionService.flushToDatabase(projectId);

        verify(rackRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(rackCache, never()).evictRack(projectId);
    }

    @Test
    void flushAllDirtySkipsCleanProjectsAndFlushesDirtyOnes() {
        UUID dirtyProjectId = UUID.randomUUID();
        UUID cleanProjectId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), dirtyProjectId);

        when(rackCache.getCachedProjectIds()).thenReturn(Set.of(dirtyProjectId, cleanProjectId));
        when(rackCache.isDirty(dirtyProjectId)).thenReturn(true);
        when(rackCache.isDirty(cleanProjectId)).thenReturn(false);
        when(rackCache.getCachedRack(dirtyProjectId)).thenReturn(Optional.of(rack));

        rackSessionService.flushAllDirty();

        verify(rackRepository).save(rack);
        verify(rackCache).clearDirty(dirtyProjectId);
        verify(rackCache, never()).getCachedRack(cleanProjectId);
    }

    @Test
    void flushAllDirtyContinuesWhenAFlushFails() {
        UUID dirtyProjectId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), dirtyProjectId);

        when(rackCache.getCachedProjectIds()).thenReturn(Set.of(dirtyProjectId));
        when(rackCache.isDirty(dirtyProjectId)).thenReturn(true);
        when(rackCache.getCachedRack(dirtyProjectId)).thenReturn(Optional.of(rack));
        when(rackRepository.save(rack)).thenThrow(new RuntimeException("boom"));

        rackSessionService.flushAllDirty();

        verify(rackCache, never()).clearDirty(dirtyProjectId);
    }

    @Test
    void releasePlaybackLockOnlyDeletesWhenCallerMatchesHolder() {
        UUID projectId = UUID.randomUUID();
        String userId = UUID.randomUUID().toString();
        String playbackKey = "playback_lock:" + projectId;

        when(lockPort.getLockHolder(playbackKey)).thenReturn(userId);

        rackSessionService.releasePlaybackLockIfHolder(projectId, userId);

        verify(lockPort).deleteLock(playbackKey);
    }

    @Test
    void releasePlaybackLockSkipsNonOwner() {
        UUID projectId = UUID.randomUUID();
        String playbackKey = "playback_lock:" + projectId;

        when(lockPort.getLockHolder(playbackKey)).thenReturn("someone-else");

        rackSessionService.releasePlaybackLockIfHolder(projectId, UUID.randomUUID().toString());

        verify(lockPort, never()).deleteLock(playbackKey);
    }

    @Test
    void releaseAllChannelLocksReleasesOnlyMatchingLocks() {
        UUID projectId = UUID.randomUUID();
        String userId = UUID.randomUUID().toString();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        Channel first = new Channel(UUID.randomUUID(), rack.getRackId(), "Kick", UUID.randomUUID(), 0);
        Channel second = new Channel(UUID.randomUUID(), rack.getRackId(), "Snare", UUID.randomUUID(), 1);
        rack.setChannels(List.of(first, second));

        String firstKey = "channel_lock:" + projectId + ":" + first.getChannelId();
        String secondKey = "channel_lock:" + projectId + ":" + second.getChannelId();

        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.of(rack));
        when(lockPort.getLockHolder(firstKey)).thenReturn(userId);
        when(lockPort.getLockHolder(secondKey)).thenReturn("other-user");

        rackSessionService.releaseAllChannelLocks(projectId, userId);

        verify(lockPort).releaseLock(firstKey, userId);
        verify(lockPort, never()).releaseLock(secondKey, userId);
    }
}