package org.eci.ZwingBackend.shared.websocket;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.project.application.port.in.ManagingProjectsCase;
import org.eci.ZwingBackend.project.domain.model.Project;
import org.eci.ZwingBackend.shared.websocket.dto.ProjectRoomMessage;
import org.eci.ZwingBackend.shared.websocket.dto.UserPresenceMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Handles all WebSocket messages related to project rooms.
 *
 * Flow:
 * 1. User opens a project → frontend sends to /app/project/{projectId}/join
 * 2. Server validates membership, then broadcasts to /topic/project/{projectId}/presence
 *    so everyone in the room sees who joined.
 * 3. When user leaves (browser close / explicit leave), frontend sends to
 *    /app/project/{projectId}/leave and server broadcasts departure.
 *
 * The {projectId} in the path is a DestinationVariable — Spring extracts it
 * automatically from the STOMP destination string, same idea as @PathVariable in REST.
 */
@Controller
@AllArgsConstructor
public class ProjectWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ManagingProjectsCase managingProjectsCase;

    /**
     * Client sends to: /app/project/{projectId}/join
     * Server broadcasts to: /topic/project/{projectId}/presence
     */
    @MessageMapping("/project/{projectId}/join")
    public void joinProject(@DestinationVariable String projectId, SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String email = (String) headerAccessor.getSessionAttributes().get("email");

        // Verify the user is actually a member of this project before letting them in
        try {
            Project project = managingProjectsCase.getProjectById(UUID.fromString(projectId), UUID.fromString(userId)
            );

            // Broadcast to everyone already in the room that this user joined
            UserPresenceMessage presence = new UserPresenceMessage(
                    userId, email, "JOINED", project.getProjectName()
            );
            messagingTemplate.convertAndSend(
                    "/topic/project/" + projectId + "/presence",
                    presence
            );

            System.out.println("[WS] " + email + " joined project " + projectId);

        } catch (RuntimeException e) {
            System.err.println("[WS ERROR] Failed to join project: " + e.getMessage());
            e.printStackTrace();

            messagingTemplate.convertAndSendToUser(userId, "/queue/errors", "Access denied to project: " + projectId);
        }
    }

    /**
     * Client sends to: /app/project/{projectId}/leave
     * Server broadcasts to: /topic/project/{projectId}/presence
     */
    @MessageMapping("/project/{projectId}/leave")
    public void leaveProject(
            @DestinationVariable String projectId,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String email = (String) headerAccessor.getSessionAttributes().get("email");

        UserPresenceMessage presence = new UserPresenceMessage(
                userId, email, "LEFT", null
        );
        messagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/presence",
                presence
        );

        System.out.println("[WS] " + email + " left project " + projectId);
    }

    /**
     * Generic project message — you'll extend this for grid toggles, canvas strokes, etc.
     * Client sends to: /app/project/{projectId}/message
     * Server broadcasts to: /topic/project/{projectId}/updates
     */
    @MessageMapping("/project/{projectId}/message")
    public void handleProjectMessage(
            @DestinationVariable String projectId,
            @Payload ProjectRoomMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String email = (String) headerAccessor.getSessionAttributes().get("email");

        message.setSenderId(userId);
        message.setSenderEmail(email);

        messagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/updates",
                message
        );
    }
}