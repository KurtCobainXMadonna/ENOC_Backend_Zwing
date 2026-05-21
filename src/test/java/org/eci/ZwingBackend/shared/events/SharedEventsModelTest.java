package org.eci.ZwingBackend.shared.events;

import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.rack.infraestructure.websocket.dto.ChannelLockPayload;
import org.eci.ZwingBackend.rack.infraestructure.websocket.dto.RackEvent;
import org.eci.ZwingBackend.rack.infraestructure.websocket.dto.StepToggledPayload;
import org.eci.ZwingBackend.shared.events.commands.ChannelLockCommand;
import org.eci.ZwingBackend.shared.events.commands.ChannelMutationCommand;
import org.eci.ZwingBackend.shared.events.commands.PlaybackCommand;
import org.eci.ZwingBackend.shared.events.commands.RackQueryCommand;
import org.eci.ZwingBackend.shared.events.results.ChannelLockResult;
import org.eci.ZwingBackend.shared.events.results.ChannelMutationResult;
import org.eci.ZwingBackend.shared.events.results.PlaybackResult;
import org.eci.ZwingBackend.shared.events.results.RackErrorResult;
import org.eci.ZwingBackend.shared.events.results.RackQueryResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SharedEventsModelTest {

    @Test
    void userDeletedEventExposesUserId() {
        UUID userId = UUID.randomUUID();

        assertThat(new UserDeletedEvent(userId).getUserId()).isEqualTo(userId);
    }

    @Test
    void rackCommandsExposeCommonFields() {
        RackQueryCommand.LoadRack loadRack = new RackQueryCommand.LoadRack("project", "user", "mail@example.com");
        PlaybackCommand.StartPlayback startPlayback = new PlaybackCommand.StartPlayback("project", "user", "mail@example.com");
        PlaybackCommand.StopPlayback stopPlayback = new PlaybackCommand.StopPlayback("project", "user", "mail@example.com");
        ChannelLockCommand.AcquireLock acquireLock = new ChannelLockCommand.AcquireLock("project", "user", "mail@example.com", "channel");
        ChannelLockCommand.ReleaseLock releaseLock = new ChannelLockCommand.ReleaseLock("project", "user", "mail@example.com", "channel");

        assertThat(loadRack.getProjectId()).isEqualTo("project");
        assertThat(startPlayback.getUserEmail()).isEqualTo("mail@example.com");
        assertThat(stopPlayback.getUserId()).isEqualTo("user");
        assertThat(acquireLock.getChannelId()).isEqualTo("channel");
        assertThat(releaseLock.getChannelId()).isEqualTo("channel");
    }

    @Test
    void channelMutationCommandExposesFields() {
        ChannelMutationCommand.AddChannel add = new ChannelMutationCommand.AddChannel("project", "user", "mail@example.com", "Kick", "sound");
        ChannelMutationCommand.RemoveChannel remove = new ChannelMutationCommand.RemoveChannel("project", "user", "mail@example.com", "channel");
        ChannelMutationCommand.UpdateChannel update = new ChannelMutationCommand.UpdateChannel("project", "user", "mail@example.com", "channel", "Kick 2", "sound", 0.5f, true);
        ChannelMutationCommand.ToggleStep toggle = new ChannelMutationCommand.ToggleStep("project", "user", "mail@example.com", "channel", 4);
        ChannelMutationCommand.UpdateBpm bpm = new ChannelMutationCommand.UpdateBpm("project", "user", "mail@example.com", 128);

        assertThat(add.getName()).isEqualTo("Kick");
        assertThat(remove.getChannelId()).isEqualTo("channel");
        assertThat(update.getVolume()).isEqualTo(0.5f);
        assertThat(toggle.getStepIndex()).isEqualTo(4);
        assertThat(bpm.getBpm()).isEqualTo(128);
    }

    @Test
    void resultModelsExposePayloads() {
        UUID projectId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        Channel channel = new Channel(channelId, rack.getRackId(), "Kick", UUID.randomUUID(), 0);

        ChannelLockResult.LockAcquired acquired = new ChannelLockResult.LockAcquired("project", "user", channelId, "mail@example.com");
        ChannelLockResult.LockReleased released = new ChannelLockResult.LockReleased("project", "user", channelId.toString());
        ChannelLockResult.LockDenied denied = new ChannelLockResult.LockDenied("project", "user", channelId.toString(), "holder");
        ChannelMutationResult.ChannelAdded added = new ChannelMutationResult.ChannelAdded("project", "user", channel);
        ChannelMutationResult.ChannelRemoved removed = new ChannelMutationResult.ChannelRemoved("project", "user", channelId.toString());
        ChannelMutationResult.ChannelUpdated updated = new ChannelMutationResult.ChannelUpdated("project", "user", channel);
        ChannelMutationResult.StepToggled toggled = new ChannelMutationResult.StepToggled("project", "user", channelId.toString(), 2, true);
        ChannelMutationResult.BpmUpdated bpmUpdated = new ChannelMutationResult.BpmUpdated("project", "user", 140);
        PlaybackResult.PlaybackStarted started = new PlaybackResult.PlaybackStarted("project", "user");
        PlaybackResult.PlaybackStopped stopped = new PlaybackResult.PlaybackStopped("project", "user");
        RackQueryResult.RackStateLoaded rackStateLoaded = new RackQueryResult.RackStateLoaded("project", "user", rack, "target-user");
        RackErrorResult error = new RackErrorResult("project", "user", "boom", "target-user");

        assertThat(acquired.getLockedByEmail()).isEqualTo("mail@example.com");
        assertThat(released.getChannelId()).isEqualTo(channelId.toString());
        assertThat(denied.getCurrentHolder()).isEqualTo("holder");
        assertThat(added.getChannel()).isSameAs(channel);
        assertThat(removed.getChannelId()).isEqualTo(channelId.toString());
        assertThat(updated.getChannel()).isSameAs(channel);
        assertThat(toggled.getStepIndex()).isEqualTo(2);
        assertThat(bpmUpdated.getBpm()).isEqualTo(140);
        assertThat(started.getTriggeredBy()).isEqualTo("user");
        assertThat(stopped.getProjectId()).isEqualTo("project");
        assertThat(rackStateLoaded.getTargetUserId()).isEqualTo("target-user");
        assertThat(error.getErrorMessage()).isEqualTo("boom");
    }

    @Test
    void websocketPayloadModelsExposeFields() {
        UUID channelId = UUID.randomUUID();
        RackEvent event = new RackEvent("CHANNEL_ADDED", Map.of("id", 1), "user");
        ChannelLockPayload lockPayload = new ChannelLockPayload(channelId, "user", "mail@example.com");
        StepToggledPayload stepPayload = new StepToggledPayload(channelId, 3, true);

        assertThat(event.getType()).isEqualTo("CHANNEL_ADDED");
        assertThat(lockPayload.getLockedByEmail()).isEqualTo("mail@example.com");
        assertThat(stepPayload.isNewValue()).isTrue();
    }
}