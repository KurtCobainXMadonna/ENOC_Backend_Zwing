package org.eci.ZwingBackend.rack.application.service;

import org.eci.ZwingBackend.rack.application.port.out.ChannelLockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelLockServiceTest {

    @Mock
    private ChannelLockPort lockPort;

    private ChannelLockService channelLockService;

    @BeforeEach
    void setUp() {
        channelLockService = new ChannelLockService(lockPort);
    }

    @Test
    void acquireAndReleaseChannelLockUseExpectedKey() {
        UUID projectId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(lockPort.acquireLock("channel_lock:" + projectId + ":" + channelId, userId.toString(), 30)).thenReturn(true);

        channelLockService.acquireChannelLock(projectId, channelId, userId);
        channelLockService.releaseChannelLock(projectId, channelId, userId);
        channelLockService.getChannelLockHolder(projectId, channelId);

        verify(lockPort).acquireLock("channel_lock:" + projectId + ":" + channelId, userId.toString(), 30);
        verify(lockPort).releaseLock("channel_lock:" + projectId + ":" + channelId, userId.toString());
        verify(lockPort).getLockHolder("channel_lock:" + projectId + ":" + channelId);
    }

    @Test
    void acquirePlaybackLockThrowsWhenAlreadyActive() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(lockPort.acquireLock("playback_lock:" + projectId, userId.toString(), 300)).thenReturn(false);

        assertThatThrownBy(() -> channelLockService.acquirePlaybackLock(projectId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Playback is already active.");
    }

    @Test
    void releaseAndCheckPlaybackLockUseExpectedKey() {
        UUID projectId = UUID.randomUUID();
        channelLockService.releasePlaybackLock(projectId);
        channelLockService.isPlaybackLocked(projectId);

        verify(lockPort).deleteLock("playback_lock:" + projectId);
        verify(lockPort).lockExists("playback_lock:" + projectId);
    }
}