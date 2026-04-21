package org.eci.ZwingBackend.shared.websocket;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.presence.application.port.in.ManagePresenceCase;
import org.eci.ZwingBackend.presence.domain.model.Presence;
import org.eci.ZwingBackend.presence.infrastructure.websocket.dto.PresenceEvent;
import org.eci.ZwingBackend.rack.application.service.RackSessionService;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class WebSocketEventListener {
    private final RackSessionService rackSessionService;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ManagePresenceCase presenceCase;

    private static final String SESSION_PREFIX = "ws_session:";

    public Presence registerUserInProject(String sessionId, String userId, String projectId,
                                          String email, String displayName) {
        String sessionData = projectId + "::" + userId;
        redisTemplate.opsForValue().set(SESSION_PREFIX + sessionId, sessionData);

        UUID projectUuid = UUID.fromString(projectId);
        Presence presence = presenceCase.userJoined(projectUuid, userId, email, displayName);

        broadcastPresence(projectUuid, "JOINED", userId);
        return presence;
    }

    public void unregisterUserFromProject(String sessionId, String userId, String projectId) {
        redisTemplate.delete(SESSION_PREFIX + sessionId);

        UUID projectUuid = UUID.fromString(projectId);
        boolean wasLastUser = presenceCase.userLeft(projectUuid, userId);

        broadcastPresence(projectUuid, "LEFT", userId);

        if (wasLastUser) {
            flushProject(projectUuid);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Map<String, Object> sessionAttributes = event.getMessage().getHeaders().get("simpSessionAttributes", Map.class);

        if (sessionAttributes == null) return;

        String userId = (String) sessionAttributes.get("userId");
        String email = (String) sessionAttributes.get("email");
        String sessionId = event.getSessionId();

        if (userId == null || sessionId == null) return;

        // Check if the session mapping still exists.
        // If unregisterUserFromProject already ran (explicit leave), the key is gone → skip.
        String sessionData = redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
        if (sessionData == null) {
            log.debug("[Presence] Session {} already cleaned up by explicit leave. Skipping disconnect handler.", sessionId);
            return;
        }

        String[] parts = sessionData.split("::");
        if (parts.length != 2) return;

        String projectId = parts[0];
        redisTemplate.delete(SESSION_PREFIX + sessionId);

        log.info("[Presence] User {} ({}) disconnected from project {}.", userId, email, projectId);

        UUID projectUuid = UUID.fromString(projectId);

        // Release locks held by this user (unchanged from previous behavior).
        try {
            rackSessionService.releasePlaybackLockIfHolder(projectUuid, userId);
            rackSessionService.releaseAllChannelLocks(projectUuid, userId);
        } catch (Exception e) {
            log.error("[Presence] Error releasing locks for user {} in project {}: {}",
                    userId, projectId, e.getMessage());
        }

        // Broadcast lock release on rack topic so other clients clear any "locked by X" UI.
        messagingTemplate.convertAndSend(
                "/topic/rack/" + projectId,
                Map.of("type", "USER_DISCONNECTED",
                        "payload", Map.of("userId", userId),
                        "triggeredBy", userId)
        );

        // Update presence and flush if last user.
        boolean wasLastUser = presenceCase.userLeft(projectUuid, userId);
        broadcastPresence(projectUuid, "LEFT", userId);

        if (wasLastUser) {
            flushProject(projectUuid);
        }
    }

    private void broadcastPresence(UUID projectId, String type, String changedUserId) {
        PresenceEvent event = new PresenceEvent(
                type,
                changedUserId,
                presenceCase.getRoster(projectId)
        );
        messagingTemplate.convertAndSend("/topic/project/" + projectId + "/presence", event);
    }

    private void flushProject(UUID projectId) {
        log.info("[Presence] Last user left project {}. Flushing rack state to PostgreSQL.", projectId);
        try {
            rackSessionService.flushToDatabase(projectId);
        } catch (Exception e) {
            log.error("[Presence] Failed to flush rack state for project {}: {}",
                    projectId, e.getMessage());
        }
    }
}
