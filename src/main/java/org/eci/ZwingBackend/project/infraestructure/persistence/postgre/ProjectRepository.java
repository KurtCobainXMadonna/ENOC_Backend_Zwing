package org.eci.ZwingBackend.project.infraestructure.persistence.postgre;

import org.eci.ZwingBackend.project.infraestructure.persistence.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    List<ProjectEntity> findByCollaborators_UserId(UUID collaboratorId);
    List<ProjectEntity> findByProjectOwner_UserId(UUID ownerId);
}
