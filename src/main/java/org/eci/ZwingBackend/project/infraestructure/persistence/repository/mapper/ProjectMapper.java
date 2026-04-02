package org.eci.ZwingBackend.project.infraestructure.persistence.repository.mapper;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.auth.infraestructure.persistence.repository.mapper.UserAuthMapper;
import org.eci.ZwingBackend.project.domain.model.Project;
import org.eci.ZwingBackend.project.infraestructure.persistence.entity.ProjectEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class ProjectMapper {
    private final UserAuthMapper userAuthMapper;

    public ProjectEntity toEntity(Project project) {
        ProjectEntity entity = new ProjectEntity();
        entity.setProjectName(project.getProjectName());
        entity.setProjectId(project.getProjectId());
        entity.setChannelRackId(project.getChannelRackId());

        if (project.getProjectOwner() != null) {
            entity.setProjectOwner(userAuthMapper.toEntity(project.getProjectOwner()));
        }
        if (project.getCollaborators() != null) {
            entity.setCollaborators(project.getCollaborators().stream()
                    .map(userAuthMapper::toEntity)
                    .collect(Collectors.toSet()));
        }
        return entity;
    }

    public Project toDomain(ProjectEntity entity) {
        Project project = new Project(entity.getProjectName());
        project.setProjectId(entity.getProjectId());
        project.setChannelRackId(entity.getChannelRackId());

        if (entity.getProjectOwner() != null) {
            project.setProjectOwner(userAuthMapper.toDomain(entity.getProjectOwner()));
        }
        if (entity.getCollaborators() != null) {
            project.setCollaborators(entity.getCollaborators().stream()
                    .map(userAuthMapper::toDomain)
                    .collect(Collectors.toSet()));
        }
        return project;
    }
}
