package org.eci.ZwingBackend.rack.infraestructure.websocket;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.port.in.ChannelLockCase;
import org.eci.ZwingBackend.rack.application.port.in.ManageChannelCase;
import org.eci.ZwingBackend.rack.application.port.in.ManageRackCase;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.rack.infraestructure.websocket.dto.ChannelLockPayload;
import org.eci.ZwingBackend.rack.infraestructure.websocket.dto.RackEvent;
import org.eci.ZwingBackend.rack.infraestructure.websocket.dto.StepToggledPayload;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
@AllArgsConstructor
public class RackWebSocketController {

    private final SimpMessagingTemplate messaging;
    private final ManageRackCase manageRackCase;
    private final ManageChannelCase manageChannelCase;
    private final ChannelLockCase channelLockCase;

    private static final String RACK_TOPIC = "/topic/rack/";
    private static final String ERROR_QUEUE = "/queue/errors";

    // ── INITIAL LOAD ──────────────────────────────────────────────────────────

    @MessageMapping("/rack/{projectId}/load")
    public void loadRack(@DestinationVariable String projectId,
                         SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        try {
            ChannelRack rack = manageRackCase.getRackByProject(UUID.fromString(projectId));
            messaging.convertAndSendToUser(userId, "/queue/rack/state",
                    new RackEvent("RACK_STATE", rack, userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    // ── CHANNEL ADD / REMOVE ──────────────────────────────────────────────────

    @MessageMapping("/rack/{projectId}/channel/add")
    public void addChannel(@DestinationVariable String projectId,
                           @Payload Map<String, String> payload,
                           SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        try {
            Channel channel = manageChannelCase.addChannel(
                    UUID.fromString(projectId),
                    payload.get("name"),
                    UUID.fromString(payload.get("soundId")),
                    UUID.fromString(userId));
            broadcast(projectId, new RackEvent("CHANNEL_ADDED", channel, userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    @MessageMapping("/rack/{projectId}/channel/{channelId}/remove")
    public void removeChannel(@DestinationVariable String projectId,
                              @DestinationVariable String channelId,
                              SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        try {
            manageChannelCase.removeChannel(
                    UUID.fromString(projectId), UUID.fromString(channelId), UUID.fromString(userId));
            broadcast(projectId, new RackEvent("CHANNEL_REMOVED",
                    Map.of("channelId", channelId), userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    // ── GRID TOGGLE ───────────────────────────────────────────────────────────

    @MessageMapping("/rack/{projectId}/channel/{channelId}/step")
    public void toggleStep(@DestinationVariable String projectId,
                           @DestinationVariable String channelId,
                           @Payload Map<String, Integer> payload,
                           SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        try {
            int stepIndex = payload.get("stepIndex");
            boolean newValue = manageChannelCase.toggleStep(
                    UUID.fromString(projectId), UUID.fromString(channelId),
                    stepIndex, UUID.fromString(userId));
            broadcast(projectId, new RackEvent("STEP_TOGGLED",
                    new StepToggledPayload(UUID.fromString(channelId), stepIndex, newValue), userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    // ── CHANNEL LOCK ──────────────────────────────────────────────────────────

    @MessageMapping("/rack/{projectId}/channel/{channelId}/lock")
    public void lockChannel(@DestinationVariable String projectId,
                            @DestinationVariable String channelId,
                            SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);
        try {
            boolean acquired = channelLockCase.acquireChannelLock(
                    UUID.fromString(projectId), UUID.fromString(channelId), UUID.fromString(userId));

            if (acquired) {
                broadcast(projectId, new RackEvent("CHANNEL_LOCKED",
                        new ChannelLockPayload(UUID.fromString(channelId), userId, email), userId));
            } else {
                String holder = channelLockCase.getChannelLockHolder(
                        UUID.fromString(projectId), UUID.fromString(channelId));
                sendError(userId, "LOCK_DENIED: Channel is being edited by user " + holder);
            }
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    @MessageMapping("/rack/{projectId}/channel/{channelId}/unlock")
    public void unlockChannel(@DestinationVariable String projectId,
                              @DestinationVariable String channelId,
                              SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        try {
            channelLockCase.releaseChannelLock(
                    UUID.fromString(projectId), UUID.fromString(channelId), UUID.fromString(userId));
            broadcast(projectId, new RackEvent("CHANNEL_UNLOCKED",
                    Map.of("channelId", channelId), userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    @MessageMapping("/rack/{projectId}/channel/{channelId}/update")
    public void updateChannel(@DestinationVariable String projectId,
                              @DestinationVariable String channelId,
                              @Payload Map<String, Object> payload,
                              SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        try {
            String soundIdStr = (String) payload.get("soundId");
            Channel updated = manageChannelCase.updateChannel(
                    UUID.fromString(projectId),
                    UUID.fromString(channelId),
                    (String) payload.get("name"),
                    soundIdStr != null ? UUID.fromString(soundIdStr) : null,
                    payload.get("volume") != null ? ((Number) payload.get("volume")).floatValue() : 1.0f,
                    payload.get("active") == null || (Boolean) payload.get("active"),
                    UUID.fromString(userId));
            broadcast(projectId, new RackEvent("CHANNEL_UPDATED", updated, userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    // ── PLAYBACK LOCK ─────────────────────────────────────────────────────────

    @MessageMapping("/rack/{projectId}/playback/start")
    public void startPlayback(@DestinationVariable String projectId,
                              SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        try {
            if (channelLockCase.isPlaybackLocked(UUID.fromString(projectId))) {
                sendError(userId, "Playback is already active.");
                return;
            }
            channelLockCase.acquirePlaybackLock(UUID.fromString(projectId), UUID.fromString(userId));
            broadcast(projectId, new RackEvent("PLAYBACK_STARTED",
                    Map.of("startedBy", userId), userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    @MessageMapping("/rack/{projectId}/playback/stop")
    public void stopPlayback(@DestinationVariable String projectId,
                             SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        try {
            if (!channelLockCase.isPlaybackLocked(UUID.fromString(projectId))) {
                sendError(userId, "Playback is not active.");
                return;
            }
            channelLockCase.releasePlaybackLock(UUID.fromString(projectId));
            broadcast(projectId, new RackEvent("PLAYBACK_STOPPED",
                    Map.of("stoppedBy", userId), userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void broadcast(String projectId, RackEvent event) {
        messaging.convertAndSend(RACK_TOPIC + projectId, event);
    }

    private void sendError(String userId, String message) {
        messaging.convertAndSendToUser(userId, ERROR_QUEUE, "ERROR: " + message);
    }

    private String getUserId(SimpMessageHeaderAccessor headers) {
        return (String) headers.getSessionAttributes().get("userId");
    }

    private String getEmail(SimpMessageHeaderAccessor headers) {
        return (String) headers.getSessionAttributes().get("email");
    }
}
