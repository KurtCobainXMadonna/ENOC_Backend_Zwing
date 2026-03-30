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

    public ProjectEntity toEntity(Project project){
        ProjectEntity newProjectEn = new ProjectEntity();
        newProjectEn.setProjectName(project.getProjectName());
        newProjectEn.setProjectId(project.getProjectId());
        if (project.getProjectOwner() != null){
            newProjectEn.setProjectOwner(userAuthMapper.toEntity(project.getProjectOwner()));
        }
        if (project.getCollaborators() != null) {
            newProjectEn.setCollaborators(project.getCollaborators().stream().map(userAuthMapper::toEntity).collect(Collectors.toSet()));
        }


        return newProjectEn;
    }

    public Project toDomain(ProjectEntity entity) {
        Project newProject = new Project(entity.getProjectName());
        newProject.setProjectId(entity.getProjectId());
        if (entity.getProjectOwner() != null) {
            newProject.setProjectOwner(userAuthMapper.toDomain(entity.getProjectOwner()));
        }
        if (entity.getCollaborators() != null) {
            newProject.setCollaborators(entity.getCollaborators().stream().map(userAuthMapper::toDomain).collect(Collectors.toSet()));
        }

        return newProject;
    }
}
