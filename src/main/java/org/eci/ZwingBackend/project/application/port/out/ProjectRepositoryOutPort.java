package org.eci.ZwingBackend.project.application.port.out;

import org.eci.ZwingBackend.project.domain.model.Project;

import java.util.List;
import java.util.UUID;

public interface ProjectRepositoryOutPort {
    void save(Project project);
    void delete(Project project);
    Project getProjectById(UUID id);

    List<Project> getProjectsByCollaborator(UUID collaboratorId);
    List<Project> getProjectsByOwner(UUID ownerId);
}
