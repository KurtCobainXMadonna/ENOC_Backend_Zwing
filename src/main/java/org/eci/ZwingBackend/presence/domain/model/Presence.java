package org.eci.ZwingBackend.presence.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Presence {
    private UUID projectId;
    private String userId;
    private String email;
    private String displayName;
    private String color;
    private Instant connectedAt;
}
