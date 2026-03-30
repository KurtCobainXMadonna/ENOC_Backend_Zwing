package org.eci.ZwingBackend.project.application.service;

import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.project.application.port.in.HandleUserDeletionCase;
import org.eci.ZwingBackend.project.application.port.in.ManagingCollaboratorCase;
import org.eci.ZwingBackend.project.application.port.in.ManagingProjectsCase;
import org.eci.ZwingBackend.project.application.port.out.ProjectRepositoryOutPort;
import org.eci.ZwingBackend.project.application.port.out.UserLookupPort;
import org.eci.ZwingBackend.project.domain.model.Project;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ProjectService implements ManagingProjectsCase, ManagingCollaboratorCase, HandleUserDeletionCase {
    private ProjectRepositoryOutPort projectRepository;
    private final UserLookupPort userLookupPort;

    @Override
    public Project createProject(String name, UUID ownerId) {
        User owner = userLookupPort.getUserById(ownerId);
        if (owner == null){
            throw new RuntimeException("Owner user not found");
        }

        Project project = new Project(name);
        project.setProjectOwner(owner);
        project.setProjectId(UUID.randomUUID());
        projectRepository.save(project);
        return project;
    }

    @Override
    public void deleteProject(UUID projectId, UUID requesterId) {
        Project project = projectRepository.getProjectById(projectId);
        if (!project.getProjectOwner().getUserId().equals(requesterId)) {
            throw new RuntimeException("Unauthorized: Only the project owner can delete this project.");
        }

        projectRepository.delete(project);
    }

    @Override
    public void addCollaborator(UUID projectId, String email) {
        Project project = projectRepository.getProjectById(projectId);
        User collaborator = userLookupPort.getUserByEmail(email);
        if (collaborator == null) {
            throw new RuntimeException("User with this email does not exist in the system.");
        }

        project.addCollaborator(collaborator);
        projectRepository.save(project);
    }

    @Override
    public void deleteCollaborator(UUID projectId, UUID collaboratorId, UUID requesterId) {
        Project project = projectRepository.getProjectById(projectId);
        if (!project.getProjectOwner().getUserId().equals(requesterId)) {
            throw new RuntimeException("Unauthorized: Only the project owner can remove collaborators.");
        }
        project.removeCollaborator(collaboratorId);
        projectRepository.save(project);
    }

    @Override
    @Transactional
    public void removeUserFromAllProjects(UUID deletedUserId) {
        List<Project> affectedProjects = projectRepository.getProjectsByCollaborator(deletedUserId);

        for (Project project : affectedProjects) {
            project.removeCollaborator(deletedUserId);
            projectRepository.save(project);
        }
    }

    @Override
    public List<Project> getOwnedProjects(UUID ownerId) {
        return projectRepository.getProjectsByOwner(ownerId);
    }

    @Override
    public List<Project> getCollaboratingProjects(UUID collaboratorId) {
        return projectRepository.getProjectsByCollaborator(collaboratorId);
    }

    @Override
    public Project getProjectById(UUID projectId, UUID requesterId) {
        Project project = projectRepository.getProjectById(projectId);

        boolean isOwner = project.getProjectOwner().getUserId().equals(requesterId);
        boolean isCollaborator = project.getCollaborators().stream().anyMatch(collab -> collab.getUserId().equals(requesterId));
        if (!isOwner && !isCollaborator) {
            throw new RuntimeException("Unauthorized: You are not a member of this project.");
        }
        return project;
    }
}
