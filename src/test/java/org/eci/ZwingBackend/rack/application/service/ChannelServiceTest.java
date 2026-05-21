package org.eci.ZwingBackend.rack.application.service;

import org.eci.ZwingBackend.rack.application.port.out.ChannelLockPort;
import org.eci.ZwingBackend.rack.application.port.out.RackCachePort;
import org.eci.ZwingBackend.rack.application.port.out.RackRepositoryPort;
import org.eci.ZwingBackend.rack.application.port.out.SoundLookupPort;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock
    private RackRepositoryPort rackRepository;

    @Mock
    private RackCachePort rackCache;

    @Mock
    private ChannelLockPort lockPort;

    @Mock
    private SoundLookupPort soundLookupPort;

    private ChannelService channelService;

    @BeforeEach
    void setUp() {
        channelService = new ChannelService(rackRepository, rackCache, new ChannelLockService(lockPort), soundLookupPort);
    }

    @Test
    void addChannelPersistsToCachedRack() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID soundId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        when(lockPort.lockExists("playback_lock:" + projectId)).thenReturn(false);
        when(soundLookupPort.soundExists(soundId)).thenReturn(true);
        when(soundLookupPort.getCategoryBySound(soundId)).thenReturn("KICK");
        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.of(rack));

        Channel channel = channelService.addChannel(projectId, "Kick", soundId, requesterId);

        assertThat(channel.getPosition()).isZero();
        assertThat(rack.getChannels()).containsExactly(channel);
        verify(rackCache).cacheRack(projectId, rack);
        verify(rackCache).markDirty(projectId);
    }

    @Test
    void addChannelRejectsWhenPlaybackIsLocked() {
        UUID projectId = UUID.randomUUID();
        when(lockPort.lockExists("playback_lock:" + projectId)).thenReturn(true);

        assertThatThrownBy(() -> channelService.addChannel(projectId, "Kick", UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cannot add channels during playback.");
    }

    @Test
    void addChannelRejectsWhenCategoryLimitIsReached() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID newSoundId = UUID.randomUUID();
        UUID firstSoundId = UUID.randomUUID();
        UUID secondSoundId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        rack.addChannel(new Channel(UUID.randomUUID(), rack.getRackId(), "One", firstSoundId, 0));
        rack.addChannel(new Channel(UUID.randomUUID(), rack.getRackId(), "Two", secondSoundId, 1));

        when(lockPort.lockExists("playback_lock:" + projectId)).thenReturn(false);
        when(soundLookupPort.soundExists(newSoundId)).thenReturn(true);
        when(soundLookupPort.getCategoryBySound(newSoundId)).thenReturn("KICK");
        when(soundLookupPort.getCategoryBySound(firstSoundId)).thenReturn("KICK");
        when(soundLookupPort.getCategoryBySound(secondSoundId)).thenReturn("KICK");
        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.of(rack));

        assertThatThrownBy(() -> channelService.addChannel(projectId, "Three", newSoundId, requesterId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Maximum 2 channels allowed for category: KICK");
    }

    @Test
    void removeChannelRemovesAndReleasesLock() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        Channel channel = new Channel(channelId, rack.getRackId(), "Kick", UUID.randomUUID(), 0);
        rack.addChannel(channel);

        when(lockPort.lockExists("playback_lock:" + projectId)).thenReturn(false);
        when(lockPort.getLockHolder("channel_lock:" + projectId + ":" + channelId)).thenReturn(null);
        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.of(rack));

        channelService.removeChannel(projectId, channelId, requesterId);

        assertThat(rack.getChannels()).isEmpty();
        verify(lockPort).releaseLock("channel_lock:" + projectId + ":" + channelId, requesterId.toString());
    }

    @Test
    void toggleStepUpdatesStepState() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        Channel channel = new Channel(channelId, rack.getRackId(), "Kick", UUID.randomUUID(), 0);
        rack.addChannel(channel);

        when(lockPort.lockExists("playback_lock:" + projectId)).thenReturn(false);
        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.of(rack));

        boolean newValue = channelService.toggleStep(projectId, channelId, 3, requesterId);

        assertThat(newValue).isTrue();
        assertThat(channel.getSteps()[3]).isTrue();
        verify(rackCache).markDirty(projectId);
    }

    @Test
    void updateChannelRequiresLockForCriticalChanges() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        Channel channel = new Channel(channelId, rack.getRackId(), "Kick", UUID.randomUUID(), 0);
        rack.addChannel(channel);

        when(lockPort.lockExists("playback_lock:" + projectId)).thenReturn(false);
        when(lockPort.getLockHolder("channel_lock:" + projectId + ":" + channelId)).thenReturn(requesterId.toString());
        when(soundLookupPort.soundExists(any())).thenReturn(true);
        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.of(rack));

        Channel updated = channelService.updateChannel(projectId, channelId, "New name", UUID.randomUUID(), 1.5f, false, requesterId);

        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getVolume()).isEqualTo(1.0f);
        assertThat(updated.isActive()).isFalse();
        verify(rackCache).markDirty(projectId);
    }

    @Test
    void updateChannelRejectsWrongLockHolder() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();

        when(lockPort.lockExists("playback_lock:" + projectId)).thenReturn(false);
        when(lockPort.getLockHolder("channel_lock:" + projectId + ":" + channelId)).thenReturn(UUID.randomUUID().toString());

        assertThatThrownBy(() -> channelService.updateChannel(projectId, channelId, "New name", null, 1f, true, requesterId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You do not hold the lock on this channel.");
    }
}