package org.eci.ZwingBackend.project.application.service;

import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.project.application.port.out.ProjectRepositoryOutPort;
import org.eci.ZwingBackend.project.application.port.out.UserLookupPort;
import org.eci.ZwingBackend.project.domain.model.Project;
import org.eci.ZwingBackend.rack.application.port.in.ManageRackCase;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepositoryOutPort projectRepository;

    @Mock
    private UserLookupPort userLookupPort;

    @Mock
    private ManageRackCase manageRackCase;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, userLookupPort, manageRackCase);
    }

    @Test
    void createProjectPersistsOwnerAndRack() {
        UUID ownerId = UUID.randomUUID();
        UUID rackId = UUID.randomUUID();
        User owner = user(ownerId, "Owner", "owner@example.com");
        ChannelRack rack = new ChannelRack(rackId, UUID.randomUUID());

        when(userLookupPort.getUserById(ownerId)).thenReturn(owner);
        when(manageRackCase.createRackForProject(org.mockito.ArgumentMatchers.any())).thenReturn(rack);

        Project created = projectService.createProject("My Project", ownerId);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        Project saved = captor.getValue();

        assertThat(created).isSameAs(saved);
        assertThat(created.getProjectName()).isEqualTo("My Project");
        assertThat(created.getProjectOwner()).isEqualTo(owner);
        assertThat(created.getProjectId()).isNotNull();
        assertThat(created.getChannelRackId()).isEqualTo(rackId);
    }

    @Test
    void createProjectFailsWhenOwnerDoesNotExist() {
        UUID ownerId = UUID.randomUUID();
        when(userLookupPort.getUserById(ownerId)).thenReturn(null);

        assertThatThrownBy(() -> projectService.createProject("My Project", ownerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Owner user not found");

        verify(projectRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(manageRackCase, never()).createRackForProject(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteProjectAllowsOwnerOnly() {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(user(ownerId, "Owner", "owner@example.com"));
        when(projectRepository.getProjectById(projectId)).thenReturn(project);

        projectService.deleteProject(projectId, ownerId);

        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProjectRejectsNonOwner() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(user(UUID.randomUUID(), "Owner", "owner@example.com"));
        when(projectRepository.getProjectById(projectId)).thenReturn(project);

        assertThatThrownBy(() -> projectService.deleteProject(projectId, UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unauthorized: Only the project owner can delete this project.");
    }

    @Test
    void addCollaboratorPersistsProject() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(user(UUID.randomUUID(), "Owner", "owner@example.com"));
        User collaborator = user(UUID.randomUUID(), "Collab", "collab@example.com");

        when(projectRepository.getProjectById(projectId)).thenReturn(project);
        when(userLookupPort.getUserByEmail("collab@example.com")).thenReturn(collaborator);

        projectService.addCollaborator(projectId, "collab@example.com");

        verify(projectRepository).save(project);
        assertThat(project.getCollaborators()).contains(collaborator);
    }

    @Test
    void addCollaboratorRejectsMissingUser() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(user(UUID.randomUUID(), "Owner", "owner@example.com"));

        when(projectRepository.getProjectById(projectId)).thenReturn(project);
        when(userLookupPort.getUserByEmail("missing@example.com")).thenReturn(null);

        assertThatThrownBy(() -> projectService.addCollaborator(projectId, "missing@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User with this email does not exist in the system.");
    }

    @Test
    void deleteCollaboratorAllowsOwnerOnly() {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(user(ownerId, "Owner", "owner@example.com"));
        User collaborator = user(collaboratorId, "Collab", "collab@example.com");
        project.addCollaborator(collaborator);
        when(projectRepository.getProjectById(projectId)).thenReturn(project);

        projectService.deleteCollaborator(projectId, collaboratorId, ownerId);

        verify(projectRepository).save(project);
        assertThat(project.getCollaborators()).doesNotContain(collaborator);
    }

    @Test
    void removeUserFromAllProjectsRemovesCollaboratorEverywhere() {
        UUID deletedUserId = UUID.randomUUID();
        Project first = new Project("First");
        first.setProjectId(UUID.randomUUID());
        first.setProjectOwner(user(UUID.randomUUID(), "Owner 1", "o1@example.com"));
        first.addCollaborator(user(deletedUserId, "Target", "target@example.com"));

        Project second = new Project("Second");
        second.setProjectId(UUID.randomUUID());
        second.setProjectOwner(user(UUID.randomUUID(), "Owner 2", "o2@example.com"));
        second.addCollaborator(user(deletedUserId, "Target", "target@example.com"));

        when(projectRepository.getProjectsByCollaborator(deletedUserId)).thenReturn(List.of(first, second));

        projectService.removeUserFromAllProjects(deletedUserId);

        verify(projectRepository).save(first);
        verify(projectRepository).save(second);
        assertThat(first.getCollaborators()).isEmpty();
        assertThat(second.getCollaborators()).isEmpty();
    }

    @Test
    void getProjectByIdAllowsCollaborator() {
        UUID projectId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(user(UUID.randomUUID(), "Owner", "owner@example.com"));
        project.addCollaborator(user(collaboratorId, "Collab", "collab@example.com"));
        when(projectRepository.getProjectById(projectId)).thenReturn(project);

        Project found = projectService.getProjectById(projectId, collaboratorId);

        assertThat(found).isSameAs(project);
    }

    @Test
    void getProjectByIdRejectsOutsider() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(user(UUID.randomUUID(), "Owner", "owner@example.com"));
        when(projectRepository.getProjectById(projectId)).thenReturn(project);

        assertThatThrownBy(() -> projectService.getProjectById(projectId, UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unauthorized: You are not a member of this project.");
    }

    private static User user(UUID userId, String name, String email) {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor(UUID.class, String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(userId, name, email);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}