package org.eci.ZwingBackend.shared.events.listeners;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.rack.infraestructure.websocket.dto.ChannelLockPayload;
import org.eci.ZwingBackend.rack.infraestructure.websocket.dto.RackEvent;
import org.eci.ZwingBackend.rack.infraestructure.websocket.dto.StepToggledPayload;
import org.eci.ZwingBackend.shared.events.results.*;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;


@Slf4j
@Component
@AllArgsConstructor
public class RackBroadcastListener {
    private final SimpMessagingTemplate messaging;
    private static final String RACK_TOPIC = "/topic/rack/";
    private static final String ERROR_QUEUE = "/queue/errors";

    @EventListener
    public void onLockAcquired(ChannelLockResult.LockAcquired event) {
        broadcast(event.getProjectId(), new RackEvent("CHANNEL_LOCKED", new ChannelLockPayload(event.getChannelId(), event.getTriggeredBy(), event.getLockedByEmail()), event.getTriggeredBy()));
    }
    @EventListener
    public void onLockReleased(ChannelLockResult.LockReleased event) {
        broadcast(event.getProjectId(), new RackEvent("CHANNEL_UNLOCKED", Map.of("channelId", event.getChannelId()), event.getTriggeredBy()));
    }
    @EventListener
    public void onLockDenied(ChannelLockResult.LockDenied event) {
        sendError(event.getTriggeredBy(), "LOCK_DENIED: Channel is being edited by user " + event.getCurrentHolder());
    }

    @EventListener
    public void onChannelAdded(ChannelMutationResult.ChannelAdded event) {
        broadcast(event.getProjectId(), new RackEvent("CHANNEL_ADDED", event.getChannel(), event.getTriggeredBy()));
    }
    @EventListener
    public void onChannelRemoved(ChannelMutationResult.ChannelRemoved event) {
        broadcast(event.getProjectId(), new RackEvent("CHANNEL_REMOVED", Map.of("channelId", event.getChannelId()), event.getTriggeredBy()));
    }
    @EventListener
    public void onChannelUpdated(ChannelMutationResult.ChannelUpdated event) {
        broadcast(event.getProjectId(), new RackEvent("CHANNEL_UPDATED", event.getChannel(), event.getTriggeredBy()));
    }
    @EventListener
    public void onStepToggled(ChannelMutationResult.StepToggled event) {
        broadcast(event.getProjectId(), new RackEvent("STEP_TOGGLED", new StepToggledPayload(UUID.fromString(event.getChannelId()), event.getStepIndex(), event.isNewValue()), event.getTriggeredBy()));
    }
    @EventListener
    public void onBpmUpdated(ChannelMutationResult.BpmUpdated event) {
        broadcast(event.getProjectId(), new RackEvent("BPM_UPDATED", Map.of("bpm", event.getBpm()), event.getTriggeredBy()));
    }

    @EventListener
    public void onPlaybackStarted(PlaybackResult.PlaybackStarted event) {
        broadcast(event.getProjectId(), new RackEvent("PLAYBACK_STARTED", Map.of("startedBy", event.getTriggeredBy()), event.getTriggeredBy()));
    }
    @EventListener
    public void onPlaybackStopped(PlaybackResult.PlaybackStopped event) {
        broadcast(event.getProjectId(), new RackEvent("PLAYBACK_STOPPED", Map.of("stoppedBy", event.getTriggeredBy()), event.getTriggeredBy()));
    }

    @EventListener
    public void onRackStateLoaded(RackQueryResult.RackStateLoaded event) {
        messaging.convertAndSendToUser(event.getTargetUserId(), "/queue/rack/state", new RackEvent("RACK_STATE", event.getRack(), event.getTriggeredBy()));
    }


    @EventListener
    public void onError(RackErrorResult event) {
        sendError(event.getTargetUserId(), event.getErrorMessage());
    }

    private void broadcast(String projectId, RackEvent event) {
        messaging.convertAndSend(RACK_TOPIC + projectId, event);
    }
    private void sendError(String userId, String message) {
        messaging.convertAndSendToUser(userId, ERROR_QUEUE, "ERROR: " + message);
    }
}
