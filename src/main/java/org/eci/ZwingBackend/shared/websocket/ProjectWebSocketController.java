package org.eci.ZwingBackend.shared.websocket;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.project.application.port.in.ManagingProjectsCase;
import org.eci.ZwingBackend.project.domain.model.Project;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@AllArgsConstructor
public class ProjectWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ManagingProjectsCase managingProjectsCase;
    private final WebSocketEventListener presenceTracker;

    @MessageMapping("/project/{projectId}/join")
    public void joinProject(@DestinationVariable String projectId,
                            SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String email = (String) headerAccessor.getSessionAttributes().get("email");
        String sessionId = headerAccessor.getSessionId();

        try {
            // Membership check — throws if user is not a collaborator.
            Project project = managingProjectsCase.getProjectById(
                    UUID.fromString(projectId), UUID.fromString(userId));

            // Register presence (assigns color, broadcasts roster internally).
            // displayName falls back to email inside PresenceService if null.
            presenceTracker.registerUserInProject(sessionId, userId, projectId, email, null);

            System.out.println("[WS] " + email + " joined project " + projectId
                    + " (" + project.getProjectName() + ")");

        } catch (RuntimeException e) {
            System.err.println("[WS ERROR] Failed to join project: " + e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    userId, "/queue/errors", "Access denied to project: " + projectId);
        }
    }

    @MessageMapping("/project/{projectId}/leave")
    public void leaveProject(@DestinationVariable String projectId,
                             SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String email = (String) headerAccessor.getSessionAttributes().get("email");
        String sessionId = headerAccessor.getSessionId();

        // Unregister presence (releases color, broadcasts roster, flushes if last user).
        presenceTracker.unregisterUserFromProject(sessionId, userId, projectId);

        System.out.println("[WS] " + email + " left project " + projectId);
    }
}
