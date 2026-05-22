package org.eci.ZwingBackend.shared.events.handlers;

import org.eci.ZwingBackend.rack.application.port.in.ManageChannelCase;
import org.eci.ZwingBackend.rack.application.port.in.ManageRackCase;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.shared.events.commands.ChannelMutationCommand;
import org.eci.ZwingBackend.shared.events.results.ChannelMutationResult;
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
class ChannelMutationCommandHandlerTest {

    @Mock
    private ManageChannelCase manageChannelCase;

    @Mock
    private ManageRackCase manageRackCase;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ChannelMutationCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ChannelMutationCommandHandler(manageChannelCase, manageRackCase, eventPublisher);
    }

    @Test
    void handleAddChannelPublishesSuccessEvent() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID soundId = UUID.randomUUID();
        Channel channel = new Channel(UUID.randomUUID(), UUID.randomUUID(), "Kick", soundId, 0);
        when(manageChannelCase.addChannel(projectId, "Kick", soundId, userId)).thenReturn(channel);

        handler.handleAddChannel(new ChannelMutationCommand.AddChannel(projectId.toString(), userId.toString(), "user@example.com", "Kick", soundId.toString()));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ChannelMutationResult.ChannelAdded.class);
    }

    @Test
    void handleAddChannelPublishesErrorEventOnFailure() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID soundId = UUID.randomUUID();
        when(manageChannelCase.addChannel(projectId, "Kick", soundId, userId)).thenThrow(new RuntimeException("boom"));

        handler.handleAddChannel(new ChannelMutationCommand.AddChannel(projectId.toString(), userId.toString(), "user@example.com", "Kick", soundId.toString()));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(RackErrorResult.class);
    }

    @Test
    void handleRemoveChannelPublishesSuccessEvent() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();

        handler.handleRemoveChannel(new ChannelMutationCommand.RemoveChannel(projectId.toString(), userId.toString(), "user@example.com", channelId.toString()));

        verify(manageChannelCase).removeChannel(projectId, channelId, userId);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ChannelMutationResult.ChannelRemoved.class);
    }

    @Test
    void handleUpdateChannelHandlesNullableSoundId() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        Channel channel = new Channel(channelId, UUID.randomUUID(), "Lead", null, 0);
        when(manageChannelCase.updateChannel(projectId, channelId, "Lead", null, 0.7f, true, userId)).thenReturn(channel);

        handler.handleUpdateChannel(new ChannelMutationCommand.UpdateChannel(projectId.toString(), userId.toString(), "user@example.com", channelId.toString(), "Lead", null, 0.7f, true));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ChannelMutationResult.ChannelUpdated.class);
    }

    @Test
    void handleUpdateBpmPublishesUpdatedRackBpm() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        rack.setBpm(144);
        when(manageRackCase.updateBpm(projectId, 144)).thenReturn(rack);

        handler.handleUpdateBpm(new ChannelMutationCommand.UpdateBpm(projectId.toString(), userId.toString(), "user@example.com", 144));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ChannelMutationResult.BpmUpdated.class);
    }

    @Test
    void handleToggleStepPublishesStepToggledResult() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        when(manageChannelCase.toggleStep(projectId, channelId, 3, userId)).thenReturn(true);

        handler.handleToggleStep(new ChannelMutationCommand.ToggleStep(projectId.toString(), userId.toString(), "user@example.com", channelId.toString(), 3));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ChannelMutationResult.StepToggled.class);
        verify(manageChannelCase, never()).updateChannel(projectId, channelId, "", null, 0f, false, userId);
    }
}