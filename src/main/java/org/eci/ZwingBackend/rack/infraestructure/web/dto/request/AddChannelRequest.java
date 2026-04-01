package org.eci.ZwingBackend.rack.infraestructure.web.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class AddChannelRequest {
    private String name;
    private UUID soundId;
}
