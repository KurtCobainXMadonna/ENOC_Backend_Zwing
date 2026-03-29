package org.eci.ZwingBackend.project.application.port.in;

import org.eci.ZwingBackend.project.domain.model.Project;

import java.util.List;
import java.util.UUID;

public interface ManagingProjectsCase {
    Project createProject (String name, UUID ownerId);
    void deleteProject (UUID projectId, UUID requesterId);

    List<Project> getOwnedProjects(UUID ownerId);
    List<Project> getCollaboratingProjects(UUID collaboratorId);
}
