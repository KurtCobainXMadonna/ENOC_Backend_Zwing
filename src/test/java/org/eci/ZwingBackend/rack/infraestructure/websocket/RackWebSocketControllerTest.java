package org.eci.ZwingBackend.rack.infraestructure.websocket;

import org.eci.ZwingBackend.shared.events.commands.ChannelLockCommand;
import org.eci.ZwingBackend.shared.events.commands.ChannelMutationCommand;
import org.eci.ZwingBackend.shared.events.commands.PlaybackCommand;
import org.eci.ZwingBackend.shared.events.commands.RackQueryCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RackWebSocketControllerTest {

    @Mock
    private ApplicationEventPublisher eventBus;

    private RackWebSocketController controller;

    @BeforeEach
    void setUp() {
        controller = new RackWebSocketController(eventBus);
    }

    @Test
    void publishesAllCommandTypesWithSessionAttributes() {
        UUID projectId = UUID.randomUUID();
        String userId = UUID.randomUUID().toString();
        SimpMessageHeaderAccessor headers = headers(userId, "user@example.com");

        controller.loadRack(projectId.toString(), headers);
        controller.addChannel(projectId.toString(), Map.of("name", "Kick", "soundId", UUID.randomUUID().toString()), headers);
        controller.removeChannel(projectId.toString(), UUID.randomUUID().toString(), headers);
        controller.toggleStep(projectId.toString(), UUID.randomUUID().toString(), Map.of("stepIndex", 3), headers);
        controller.lockChannel(projectId.toString(), UUID.randomUUID().toString(), headers);
        controller.unlockChannel(projectId.toString(), UUID.randomUUID().toString(), headers);
        controller.startPlayback(projectId.toString(), headers);
        controller.stopPlayback(projectId.toString(), headers);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventBus, times(8)).publishEvent(captor.capture());

        assertThat(captor.getAllValues()).anyMatch(RackQueryCommand.LoadRack.class::isInstance);
        assertThat(captor.getAllValues()).anyMatch(ChannelMutationCommand.AddChannel.class::isInstance);
        assertThat(captor.getAllValues()).anyMatch(ChannelMutationCommand.RemoveChannel.class::isInstance);
        assertThat(captor.getAllValues()).anyMatch(ChannelMutationCommand.ToggleStep.class::isInstance);
        assertThat(captor.getAllValues()).anyMatch(ChannelLockCommand.AcquireLock.class::isInstance);
        assertThat(captor.getAllValues()).anyMatch(ChannelLockCommand.ReleaseLock.class::isInstance);
        assertThat(captor.getAllValues()).anyMatch(PlaybackCommand.StartPlayback.class::isInstance);
        assertThat(captor.getAllValues()).anyMatch(PlaybackCommand.StopPlayback.class::isInstance);
    }

    @Test
    void updateChannelAndBpmUseDefaultsWhenMissing() {
        UUID projectId = UUID.randomUUID();
        String userId = UUID.randomUUID().toString();
        String channelId = UUID.randomUUID().toString();
        SimpMessageHeaderAccessor headers = headers(userId, "user@example.com");

        controller.updateChannel(projectId.toString(), channelId, new HashMap<>(Map.of("name", "Snare")), headers);
        controller.updateBpm(projectId.toString(), Map.of(), headers);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventBus, times(2)).publishEvent(captor.capture());

        ChannelMutationCommand.UpdateChannel update = (ChannelMutationCommand.UpdateChannel) captor.getAllValues().get(0);
        assertThat(update.getChannelId()).isEqualTo(channelId);
        assertThat(update.getName()).isEqualTo("Snare");
        assertThat(update.getVolume()).isEqualTo(1.0f);
        assertThat(update.isActive()).isTrue();

        ChannelMutationCommand.UpdateBpm bpm = (ChannelMutationCommand.UpdateBpm) captor.getAllValues().get(1);
        assertThat(bpm.getBpm()).isEqualTo(120);
    }

    private static SimpMessageHeaderAccessor headers(String userId, String email) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionAttributes(new HashMap<>(Map.of("userId", userId, "email", email)));
        accessor.setSessionId(UUID.randomUUID().toString());
        accessor.setLeaveMutable(true);
        return accessor;
    }
}