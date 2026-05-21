package org.eci.ZwingBackend.shared.events.handlers;

import org.eci.ZwingBackend.rack.application.port.in.ChannelLockCase;
import org.eci.ZwingBackend.rack.application.port.in.ManageChannelCase;
import org.eci.ZwingBackend.rack.application.port.in.ManageRackCase;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.shared.events.commands.ChannelLockCommand;
import org.eci.ZwingBackend.shared.events.commands.ChannelMutationCommand;
import org.eci.ZwingBackend.shared.events.commands.PlaybackCommand;
import org.eci.ZwingBackend.shared.events.commands.RackQueryCommand;
import org.eci.ZwingBackend.shared.events.results.ChannelLockResult;
import org.eci.ZwingBackend.shared.events.results.ChannelMutationResult;
import org.eci.ZwingBackend.shared.events.results.RackQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RackAndPlaybackHandlersTest {

    @Mock
    private ManageRackCase manageRackCase;

    @Mock
    private ChannelLockCase channelLockCase;

    @Mock
    private ManageChannelCase manageChannelCase;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RackQueryCommandHandler rackQueryCommandHandler;
    private PlaybackCommandHandler playbackCommandHandler;
    private ChannelMutationCommandHandler channelMutationCommandHandler;
    private ChannelLockCommandHandler channelLockCommandHandler;

    @BeforeEach
    void setUp() {
        rackQueryCommandHandler = new RackQueryCommandHandler(manageRackCase, eventPublisher);
        playbackCommandHandler = new PlaybackCommandHandler(channelLockCase, eventPublisher);
        channelMutationCommandHandler = new ChannelMutationCommandHandler(manageChannelCase, manageRackCase, eventPublisher);
        channelLockCommandHandler = new ChannelLockCommandHandler(channelLockCase, eventPublisher);
    }

    @Test
    void rackQueryHandlerPublishesLoadedStateAndErrors() {
        UUID projectId = UUID.randomUUID();
        String userId = UUID.randomUUID().toString();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        when(manageRackCase.getRackByProject(projectId)).thenReturn(rack);

        rackQueryCommandHandler.handleLoadRack(new RackQueryCommand.LoadRack(projectId.toString(), userId, "user@example.com"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(RackQueryResult.RackStateLoaded.class);
    }

    @Test
    void playbackHandlerPublishesStartedStoppedAndErrors() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(channelLockCase.isPlaybackLocked(projectId)).thenReturn(false, true, false);

        playbackCommandHandler.handleStartPlayback(new PlaybackCommand.StartPlayback(projectId.toString(), userId.toString(), "user@example.com"));
        playbackCommandHandler.handleStopPlayback(new PlaybackCommand.StopPlayback(projectId.toString(), userId.toString(), "user@example.com"));

        verify(channelLockCase).acquirePlaybackLock(projectId, userId);
        verify(channelLockCase).releasePlaybackLock(projectId);
    }

    @Test
    void channelMutationHandlerCoversAllPaths() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID soundId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        Channel channel = new Channel(channelId, UUID.randomUUID(), "Kick", soundId, 0);
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        when(manageChannelCase.addChannel(any(), any(), any(), any())).thenReturn(channel);
        when(manageChannelCase.updateChannel(any(), any(), any(), any(), any(Float.class), any(Boolean.class), any())).thenReturn(channel);
        when(manageChannelCase.toggleStep(any(), any(), any(Integer.class), any())).thenReturn(true);
        when(manageRackCase.updateBpm(any(), any(Integer.class))).thenReturn(rack);

        channelMutationCommandHandler.handleAddChannel(new ChannelMutationCommand.AddChannel(projectId.toString(), userId.toString(), "user@example.com", "Kick", soundId.toString()));
        channelMutationCommandHandler.handleRemoveChannel(new ChannelMutationCommand.RemoveChannel(projectId.toString(), userId.toString(), "user@example.com", channelId.toString()));
        channelMutationCommandHandler.handleUpdateChannel(new ChannelMutationCommand.UpdateChannel(projectId.toString(), userId.toString(), "user@example.com", channelId.toString(), "Kick 2", soundId.toString(), 0.5f, true));
        channelMutationCommandHandler.handleToggleStep(new ChannelMutationCommand.ToggleStep(projectId.toString(), userId.toString(), "user@example.com", channelId.toString(), 3));
        channelMutationCommandHandler.handleUpdateBpm(new ChannelMutationCommand.UpdateBpm(projectId.toString(), userId.toString(), "user@example.com", 128));

        verify(eventPublisher).publishEvent(any(ChannelMutationResult.ChannelAdded.class));
        verify(eventPublisher).publishEvent(any(ChannelMutationResult.ChannelRemoved.class));
        verify(eventPublisher).publishEvent(any(ChannelMutationResult.ChannelUpdated.class));
        verify(eventPublisher).publishEvent(any(ChannelMutationResult.StepToggled.class));
        verify(eventPublisher).publishEvent(any(ChannelMutationResult.BpmUpdated.class));
    }

    @Test
    void channelLockHandlerCoversAcquireAndRelease() {
        UUID projectId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(channelLockCase.acquireChannelLock(projectId, channelId, userId)).thenReturn(true);

        channelLockCommandHandler.handleAcquireLock(new ChannelLockCommand.AcquireLock(projectId.toString(), userId.toString(), "user@example.com", channelId.toString()));
        channelLockCommandHandler.handleReleaseLock(new ChannelLockCommand.ReleaseLock(projectId.toString(), userId.toString(), "user@example.com", channelId.toString()));

        verify(channelLockCase).releaseChannelLock(projectId, channelId, userId);
        verify(eventPublisher).publishEvent(any(ChannelLockResult.LockAcquired.class));
        verify(eventPublisher).publishEvent(any(ChannelLockResult.LockReleased.class));
    }
}