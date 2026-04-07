package org.eci.ZwingBackend.rack.infraestructure.web.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateChannelRequest {
    private String name;
    private UUID soundId;
    private float volume;
    private boolean active;
}
