package org.eci.ZwingBackend.rack.infraestructure.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Every outbound WebSocket message from RackWebSocketController uses this wrapper.
 * The frontend switches on 'type' to decide what state to update.
 *
 * Types: CHANNEL_ADDED, CHANNEL_REMOVED, CHANNEL_UPDATED, STEP_TOGGLED,
 *        CHANNEL_LOCKED, CHANNEL_UNLOCKED, PLAYBACK_STARTED, PLAYBACK_STOPPED,
 *        RACK_STATE, LOCK_DENIED, ERROR
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RackEvent {
    private String type;
    private Object payload;
    private String triggeredBy; // userId who caused the event
}
