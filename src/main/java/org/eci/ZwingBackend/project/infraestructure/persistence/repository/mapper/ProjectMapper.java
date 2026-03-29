package org.eci.ZwingBackend.project.infraestructure.persistence.repository.mapper;

import org.eci.ZwingBackend.project.domain.model.Project;
import org.eci.ZwingBackend.project.infraestructure.persistence.entity.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {
    public ProjectEntity toEntity(Project project){
        ProjectEntity newProjectEn = new ProjectEntity();
        newProjectEn.setProjectName(project.getProjectName());
        newProjectEn.setCollaborators(project.getCollaborators());
        newProjectEn.setProjectId(project.getProjectId());
        newProjectEn.setProjectOwner(project.getProjectOwner());

        return newProjectEn;
    }

    public Project toDomain(ProjectEntity project){
        Project newProject = new Project(project.getProjectName());
        newProject.setProjectId(project.getProjectId());
        newProject.setCollaborators(project.getCollaborators());
        newProject.setProjectOwner(project.getProjectOwner());

        return newProject;
    }
}
