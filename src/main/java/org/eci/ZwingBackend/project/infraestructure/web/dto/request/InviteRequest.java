package org.eci.ZwingBackend.project.infraestructure.web.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class InviteRequest {
    private UUID projectId;
    private String inviteeEmail;
}