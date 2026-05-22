package org.eci.ZwingBackend.shared.events.handlers;

import org.eci.ZwingBackend.rack.application.port.in.ChannelLockCase;
import org.eci.ZwingBackend.shared.events.commands.PlaybackCommand;
import org.eci.ZwingBackend.shared.events.results.PlaybackResult;
import org.eci.ZwingBackend.shared.events.results.RackErrorResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaybackCommandHandlerTest {

    @Mock
    private ChannelLockCase channelLockCase;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PlaybackCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PlaybackCommandHandler(channelLockCase, eventPublisher);
    }

    @Test
    void startPlaybackAcquiresLockAndPublishesEvent() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(channelLockCase.isPlaybackLocked(projectId)).thenReturn(false);

        handler.handleStartPlayback(new PlaybackCommand.StartPlayback(projectId.toString(), userId.toString(), "user@example.com"));

        verify(channelLockCase).acquirePlaybackLock(projectId, userId);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(PlaybackResult.PlaybackStarted.class);
    }

    @Test
    void startPlaybackPublishesErrorWhenAlreadyLocked() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(channelLockCase.isPlaybackLocked(projectId)).thenReturn(true);

        handler.handleStartPlayback(new PlaybackCommand.StartPlayback(projectId.toString(), userId.toString(), "user@example.com"));

        verify(channelLockCase, never()).acquirePlaybackLock(projectId, userId);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(RackErrorResult.class);
    }

    @Test
    void stopPlaybackReleasesLockAndPublishesEvent() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(channelLockCase.isPlaybackLocked(projectId)).thenReturn(true);

        handler.handleStopPlayback(new PlaybackCommand.StopPlayback(projectId.toString(), userId.toString(), "user@example.com"));

        verify(channelLockCase).releasePlaybackLock(projectId);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(PlaybackResult.PlaybackStopped.class);
    }

    @Test
    void stopPlaybackPublishesErrorWhenNotActive() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(channelLockCase.isPlaybackLocked(projectId)).thenReturn(false);

        handler.handleStopPlayback(new PlaybackCommand.StopPlayback(projectId.toString(), userId.toString(), "user@example.com"));

        verify(channelLockCase, never()).releasePlaybackLock(projectId);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(RackErrorResult.class);
    }
}