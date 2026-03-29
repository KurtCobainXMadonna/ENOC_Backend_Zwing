package org.eci.ZwingBackend.project.application.port.in;

import java.util.UUID;

public interface HandleUserDeletionCase {
    void removeUserFromAllProjects(UUID deletedUserId);
}