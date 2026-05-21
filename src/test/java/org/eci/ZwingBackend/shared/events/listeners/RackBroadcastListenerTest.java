package org.eci.ZwingBackend.shared.events.listeners;

import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.rack.infraestructure.websocket.dto.RackEvent;
import org.eci.ZwingBackend.shared.events.results.ChannelLockResult;
import org.eci.ZwingBackend.shared.events.results.ChannelMutationResult;
import org.eci.ZwingBackend.shared.events.results.PlaybackResult;
import org.eci.ZwingBackend.shared.events.results.RackErrorResult;
import org.eci.ZwingBackend.shared.events.results.RackQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RackBroadcastListenerTest {

    @Mock
    private SimpMessagingTemplate messaging;

    private RackBroadcastListener listener;

    @BeforeEach
    void setUp() {
        listener = new RackBroadcastListener(messaging);
    }

    @Test
    void broadcastsAndErrorsAcrossResultTypes() {
        UUID projectId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        Channel channel = new Channel(channelId, UUID.randomUUID(), "Kick", UUID.randomUUID(), 0);
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);

        listener.onLockAcquired(new ChannelLockResult.LockAcquired(projectId.toString(), "user", channelId, "mail@example.com"));
        listener.onLockReleased(new ChannelLockResult.LockReleased(projectId.toString(), "user", channelId.toString()));
        listener.onLockDenied(new ChannelLockResult.LockDenied(projectId.toString(), "user", channelId.toString(), "holder"));
        listener.onChannelAdded(new ChannelMutationResult.ChannelAdded(projectId.toString(), "user", channel));
        listener.onChannelRemoved(new ChannelMutationResult.ChannelRemoved(projectId.toString(), "user", channelId.toString()));
        listener.onChannelUpdated(new ChannelMutationResult.ChannelUpdated(projectId.toString(), "user", channel));
        listener.onStepToggled(new ChannelMutationResult.StepToggled(projectId.toString(), "user", channelId.toString(), 2, true));
        listener.onBpmUpdated(new ChannelMutationResult.BpmUpdated(projectId.toString(), "user", 128));
        listener.onPlaybackStarted(new PlaybackResult.PlaybackStarted(projectId.toString(), "user"));
        listener.onPlaybackStopped(new PlaybackResult.PlaybackStopped(projectId.toString(), "user"));
        listener.onRackStateLoaded(new RackQueryResult.RackStateLoaded(projectId.toString(), "user", rack, "target-user"));
        listener.onError(new RackErrorResult(projectId.toString(), "user", "boom", "target-user"));

        ArgumentCaptor<RackEvent> rackEventCaptor = ArgumentCaptor.forClass(RackEvent.class);
        verify(messaging, atLeastOnce()).convertAndSend(any(String.class), rackEventCaptor.capture());
        assertThat(rackEventCaptor.getAllValues()).isNotEmpty();
    }
}