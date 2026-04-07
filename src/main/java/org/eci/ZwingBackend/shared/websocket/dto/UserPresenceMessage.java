package org.eci.ZwingBackend.shared.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sent to /topic/project/{projectId}/presence whenever a user joins or leaves.
 * The frontend listens to this topic to show live "who's in this project" indicators.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPresenceMessage {
    private String userId;
    private String email;
    private String status;       // "JOINED" or "LEFT"
    private String projectName;  // null when leaving
}