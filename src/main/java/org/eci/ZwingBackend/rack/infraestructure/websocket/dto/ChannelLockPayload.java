package org.eci.ZwingBackend.rack.infraestructure.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ChannelLockPayload {
    private UUID channelId;
    private String lockedByUserId;
    private String lockedByEmail;
}
