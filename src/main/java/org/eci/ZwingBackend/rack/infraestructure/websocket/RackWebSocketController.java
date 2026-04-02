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

/**
 * All rack real-time operations flow through here.
 *
 * Client sends to:   /app/rack/{projectId}/{operation}
 * Server broadcasts: /topic/rack/{projectId}          ← entire project room sees it
 * Server errors to:  /user/queue/errors               ← only the requester sees it
 *
 * Every handler:
 *   1. Extracts userId/email from session (set by JwtHandshakeInterceptor at handshake)
 *   2. Delegates to use case (which enforces business rules + locks)
 *   3. Broadcasts RackEvent to the project topic
 *   4. On error: sends only to /user/queue/errors — never broadcast
 */
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

    /**
     * Client sends to /app/rack/{projectId}/load when entering the project room.
     * Returns the full rack state only to the requesting user (not a broadcast).
     */
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

    /** Client sends to /app/rack/{projectId}/channel/add  — payload: { name, soundId } */
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
                    UUID.fromString(userId)
            );
            broadcast(projectId, new RackEvent("CHANNEL_ADDED", channel, userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    /** Client sends to /app/rack/{projectId}/channel/{channelId}/remove */
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

    /**
     * Client sends to /app/rack/{projectId}/channel/{channelId}/step
     * Payload: { "stepIndex": 3 }
     * Most frequent operation — fires on every cell click.
     */
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

    /** Client sends to /app/rack/{projectId}/channel/{channelId}/lock */
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

    /** Client sends to /app/rack/{projectId}/channel/{channelId}/unlock */
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

    /**
     * Client sends to /app/rack/{projectId}/channel/{channelId}/update
     * Payload: { name, soundId, volume, active } — must hold the channel lock.
     */
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
                    UUID.fromString(userId)
            );
            broadcast(projectId, new RackEvent("CHANNEL_UPDATED", updated, userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    // ── PLAYBACK LOCK ─────────────────────────────────────────────────────────

    /** Client sends to /app/rack/{projectId}/playback/start — freezes the entire rack. */
    @MessageMapping("/rack/{projectId}/playback/start")
    public void startPlayback(@DestinationVariable String projectId,
                              SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        try {
            channelLockCase.acquirePlaybackLock(UUID.fromString(projectId), UUID.fromString(userId));
            broadcast(projectId, new RackEvent("PLAYBACK_STARTED",
                    Map.of("startedBy", userId), userId));
        } catch (Exception e) {
            sendError(userId, e.getMessage());
        }
    }

    /** Client sends to /app/rack/{projectId}/playback/stop — editing resumes for all. */
    @MessageMapping("/rack/{projectId}/playback/stop")
    public void stopPlayback(@DestinationVariable String projectId,
                             SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        try {
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
