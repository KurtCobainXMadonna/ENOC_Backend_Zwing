package org.eci.ZwingBackend.project.infraestructure.web;

import org.eci.ZwingBackend.project.application.port.in.InviteCollaboratorCase;
import org.eci.ZwingBackend.project.application.port.in.ManagingCollaboratorCase;
import org.eci.ZwingBackend.project.application.port.in.ManagingProjectsCase;
import org.eci.ZwingBackend.project.domain.model.Project;
import org.eci.ZwingBackend.project.infraestructure.web.dto.request.CreateProjectRequest;
import org.eci.ZwingBackend.project.infraestructure.web.dto.request.InviteRequest;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAndInviteControllerTest {

    @Mock
    private ManagingProjectsCase managingProjectsCase;

    @Mock
    private ManagingCollaboratorCase managingCollaboratorCase;

    @Mock
    private InviteCollaboratorCase inviteCollaboratorCase;

    private ProjectController projectController;
    private InviteController inviteController;

    @BeforeEach
    void setUp() {
        projectController = new ProjectController(managingProjectsCase, managingCollaboratorCase);
        inviteController = new InviteController(inviteCollaboratorCase);
    }

    @Test
    void getAllProjectsCombinesOwnedAndCollaborating() {
        UUID userId = UUID.randomUUID();
        Project owned = new Project("Owned");
        Project collab = new Project("Collab");
        when(managingProjectsCase.getOwnedProjects(userId)).thenReturn(List.of(owned));
        when(managingProjectsCase.getCollaboratingProjects(userId)).thenReturn(List.of(collab));

        ResponseEntity<GeneralResponse<Map<String, List<Project>>>> entity = projectController.getAllProjects(userId);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getData()).containsEntry("ownedProjects", List.of(owned));
        assertThat(entity.getBody().getData()).containsEntry("collaboratingProjects", List.of(collab));
    }

    @Test
    void getProjectByIdDelegatesToService() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = new Project("Project");
        when(managingProjectsCase.getProjectById(projectId, requesterId)).thenReturn(project);

        ResponseEntity<GeneralResponse<Project>> entity = projectController.getProjectById(projectId, requesterId);

        assertThat(entity.getBody().getData()).isSameAs(project);
    }

    @Test
    void createProjectDelegatesToUseCase() {
        UUID ownerId = UUID.randomUUID();
        Project project = new Project("New Project");
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("New Project");
        when(managingProjectsCase.createProject("New Project", ownerId)).thenReturn(project);

        ResponseEntity<GeneralResponse<Project>> entity = projectController.createProject(request, ownerId);

        assertThat(entity.getBody().getData()).isSameAs(project);
    }

    @Test
    void deleteProjectDelegatesToUseCase() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ResponseEntity<GeneralResponse<Void>> entity = projectController.deleteProject(projectId, requesterId);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(managingProjectsCase).deleteProject(projectId, requesterId);
    }

    @Test
    void addAndDeleteCollaboratorDelegateToUseCase() {
        UUID projectId = UUID.randomUUID();

        ResponseEntity<GeneralResponse<Void>> addEntity = projectController.addCollaborator(projectId, "ada@example.com");
        ResponseEntity<GeneralResponse<Void>> deleteEntity = projectController.deleteCollaborator(projectId, UUID.randomUUID(), UUID.randomUUID());

        assertThat(addEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(managingCollaboratorCase).addCollaborator(projectId, "ada@example.com");
    }

    @Test
    void inviteControllerReturnsInviteToken() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        InviteRequest request = new InviteRequest();
        request.setProjectId(projectId);
        when(inviteCollaboratorCase.inviteCollaborator(projectId, requesterId)).thenReturn("invite-token");

        ResponseEntity<GeneralResponse<Map<String, String>>> entity = inviteController.invite(request, requesterId);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getData()).containsEntry("inviteToken", "invite-token");
    }

    @Test
    void inviteAcceptDelegatesToUseCase() {
        UUID userId = UUID.randomUUID();

        ResponseEntity<GeneralResponse<Void>> entity = inviteController.accept("token", userId);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(inviteCollaboratorCase).acceptInvite("token", userId);
    }
}