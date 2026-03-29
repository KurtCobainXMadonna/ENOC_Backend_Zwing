package org.eci.ZwingBackend.project.application.port.in;

import java.util.UUID;

public interface ManagingCollaboratorCase {
    void addCollaborator(UUID projectId, String email); // Changed to email, removed requesterId
    void deleteCollaborator(UUID projectId, UUID collaboratorId, UUID requesterId);
}
