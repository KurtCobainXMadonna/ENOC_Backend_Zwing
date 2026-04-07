package org.eci.ZwingBackend.shared.websocket.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic payload for project room messages.
 * The 'type' field is what you'll use to differentiate operations later:
 * "GRID_TOGGLE", "CANVAS_STROKE", "CHAT_MESSAGE", etc.
 *
 * The 'payload' field is a raw JSON string so each operation type
 * can carry its own structure without needing a new DTO per operation.
 */
@Data
@NoArgsConstructor
public class ProjectRoomMessage {
    private String type;        // e.g. "GRID_TOGGLE", "CANVAS_STROKE"
    private String payload;     // JSON string — parsed by the frontend based on type
    private String senderId;    // Set by the server from session attributes, not trusted from client
    private String senderEmail;
}