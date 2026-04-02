package org.eci.ZwingBackend.shared.websocket;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.rack.application.service.RackSessionService;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.UUID;

/**
 * Handles WebSocket session disconnect events.
 *
 * When a user disconnects (browser close, network loss, explicit leave):
 *   1. Release any channel locks they held (faster than waiting for TTL)
 *   2. Release playback lock if they held it (has NO TTL — would persist forever)
 *   3. Decrement the presence counter for the project
 *   4. If counter reaches 0, flush Redis rack state → PostgreSQL
 *   5. Broadcast a LEFT presence message to the project room
 *
 * Presence counter key: project_presence:{projectId} → count of connected users
 * User-to-project mapping: ws_session:{sessionId} → projectId
 */
@Slf4j
@Component
@AllArgsConstructor
public class WebSocketEventListener {
    private final RackSessionService rackSessionService;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String PRESENCE_PREFIX = "project_presence:";
    private static final String SESSION_PREFIX = "ws_session:";

    // ── Called by ProjectWebSocketController on join/leave ─────────────────────

    /**
     * Register a user's presence in a project room.
     * Called from ProjectWebSocketController.joinProject().
     */
    public void registerUserInProject(String sessionId, String userId, String projectId) {
        // Map session → project + userId so we can clean up on disconnect
        String sessionData = projectId + "::" + userId;
        redisTemplate.opsForValue().set(SESSION_PREFIX + sessionId, sessionData);

        // Increment presence counter
        redisTemplate.opsForValue().increment(PRESENCE_PREFIX + projectId);
        log.info("[Presence] User {} joined project {}. Session: {}", userId, projectId, sessionId);
    }

    /**
     * Unregister a user's presence (explicit leave, not disconnect).
     * Called from ProjectWebSocketController.leaveProject().
     */
    public void unregisterUserFromProject(String sessionId, String userId, String projectId) {
        redisTemplate.delete(SESSION_PREFIX + sessionId);
        decrementAndFlushIfEmpty(projectId, userId);
    }

    // ── Disconnect handler ────────────────────────────────────────────────────

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Map<String, Object> sessionAttributes = event.getMessage().getHeaders()
                .get("simpSessionAttributes", Map.class);

        if (sessionAttributes == null) return;

        String userId = (String) sessionAttributes.get("userId");
        String email = (String) sessionAttributes.get("email");
        String sessionId = event.getSessionId();

        if (userId == null || sessionId == null) return;

        // Look up which project this session was in
        String sessionData = redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
        if (sessionData == null) {
            log.debug("[Presence] No project mapping for disconnected session {}.", sessionId);
            return;
        }

        String[] parts = sessionData.split("::");
        if (parts.length != 2) return;

        String projectId = parts[0];
        redisTemplate.delete(SESSION_PREFIX + sessionId);

        log.info("[Presence] User {} ({}) disconnected from project {}.", userId, email, projectId);

        // 1. Release locks
        try {
            UUID projectUuid = UUID.fromString(projectId);
            rackSessionService.releasePlaybackLockIfHolder(projectUuid, userId);
            rackSessionService.releaseAllChannelLocks(projectUuid, userId);
        } catch (Exception e) {
            log.error("[Presence] Error releasing locks for user {} in project {}: {}",
                    userId, projectId, e.getMessage());
        }

        // 2. Broadcast departure
        messagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/presence",
                Map.of("userId", userId, "email", email != null ? email : "", "status", "LEFT")
        );

        // 3. Broadcast lock releases to rack topic so other clients update their UI
        messagingTemplate.convertAndSend(
                "/topic/rack/" + projectId,
                Map.of("type", "USER_DISCONNECTED", "payload", Map.of("userId", userId), "triggeredBy", userId)
        );

        // 4. Decrement presence counter and flush if last user
        decrementAndFlushIfEmpty(projectId, userId);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void decrementAndFlushIfEmpty(String projectId, String userId) {
        Long remaining = redisTemplate.opsForValue().decrement(PRESENCE_PREFIX + projectId);

        if (remaining == null || remaining <= 0) {
            // Last user left — flush rack state to PostgreSQL
            redisTemplate.delete(PRESENCE_PREFIX + projectId);
            log.info("[Presence] Last user left project {}. Flushing rack state to PostgreSQL.", projectId);

            try {
                rackSessionService.flushToDatabase(UUID.fromString(projectId));
            } catch (Exception e) {
                log.error("[Presence] Failed to flush rack state for project {}: {}",
                        projectId, e.getMessage());
            }
        } else {
            log.info("[Presence] {} user(s) remaining in project {}.", remaining, projectId);
        }
    }
}
