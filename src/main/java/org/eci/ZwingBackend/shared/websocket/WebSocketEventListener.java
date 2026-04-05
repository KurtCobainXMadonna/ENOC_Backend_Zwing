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
 * IMPORTANT: When a user clicks "Back", TWO things happen:
 *   1. The frontend sends a STOMP /leave message → ProjectWebSocketController calls unregisterUserFromProject
 *   2. The WebSocket closes → SessionDisconnectEvent fires → handleSessionDisconnect runs
 *
 * To prevent double-decrement, unregisterUserFromProject deletes the session mapping from Redis.
 * When handleSessionDisconnect fires, it checks if the mapping still exists — if not, it skips.
 * This way, only ONE path decrements the counter.
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

    public void registerUserInProject(String sessionId, String userId, String projectId) {
        String sessionData = projectId + "::" + userId;
        redisTemplate.opsForValue().set(SESSION_PREFIX + sessionId, sessionData);
        redisTemplate.opsForValue().increment(PRESENCE_PREFIX + projectId);
        log.info("[Presence] User {} joined project {}. Session: {}", userId, projectId, sessionId);
    }

    /**
     * Explicit leave (user clicked "Back"). Cleans up session mapping AND decrements.
     * Deleting the session key prevents handleSessionDisconnect from double-decrementing.
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

        // Release locks
        try {
            UUID projectUuid = UUID.fromString(projectId);
            rackSessionService.releasePlaybackLockIfHolder(projectUuid, userId);
            rackSessionService.releaseAllChannelLocks(projectUuid, userId);
        } catch (Exception e) {
            log.error("[Presence] Error releasing locks for user {} in project {}: {}",
                    userId, projectId, e.getMessage());
        }

        // Broadcast departure
        messagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/presence",
                Map.of("userId", userId, "email", email != null ? email : "", "status", "LEFT")
        );

        // Broadcast lock release to rack topic
        messagingTemplate.convertAndSend(
                "/topic/rack/" + projectId,
                Map.of("type", "USER_DISCONNECTED", "payload", Map.of("userId", userId), "triggeredBy", userId)
        );

        // Decrement presence and flush if last user
        decrementAndFlushIfEmpty(projectId, userId);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void decrementAndFlushIfEmpty(String projectId, String userId) {
        Long remaining = redisTemplate.opsForValue().decrement(PRESENCE_PREFIX + projectId);

        if (remaining == null || remaining <= 0) {
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