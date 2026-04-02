package org.eci.ZwingBackend.rack.infraestructure.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class StepToggledPayload {
    private UUID channelId;
    private int stepIndex;
    private boolean newValue;
}
