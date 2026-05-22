package org.eci.ZwingBackend.project.application.service;

import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.project.application.port.out.InviteRepositoryPort;
import org.eci.ZwingBackend.project.application.port.out.InviteTokenGeneratorPort;
import org.eci.ZwingBackend.project.application.port.in.ManagingCollaboratorCase;
import org.eci.ZwingBackend.project.application.port.out.ProjectRepositoryOutPort;
import org.eci.ZwingBackend.project.application.port.out.UserLookupPort;
import org.eci.ZwingBackend.project.domain.model.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private ProjectRepositoryOutPort projectRepository;

    @Mock
    private InviteRepositoryPort inviteRepository;

    @Mock
    private UserLookupPort userLookupPort;

    @Mock
    private ManagingCollaboratorCase managingCollaboratorCase;

    @Mock
    private InviteTokenGeneratorPort inviteTokenGeneratorPort;

    private InviteService inviteService;

    @BeforeEach
    void setUp() {
        inviteService = new InviteService(projectRepository, inviteRepository, userLookupPort, managingCollaboratorCase, inviteTokenGeneratorPort);
    }

    @Test
    void inviteCollaboratorGeneratesAndPersistsInviteToken() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(new User(requesterId, "Owner", "owner@example.com"));

        when(projectRepository.getProjectById(projectId)).thenReturn(project);
        when(inviteTokenGeneratorPort.generateToken()).thenReturn("INVITE01");

        String token = inviteService.inviteCollaborator(projectId, requesterId);

        assertThat(token).isEqualTo("INVITE01");
        verify(inviteRepository).saveInvite("INVITE01", projectId, 15L * 60);
    }

    @Test
    void inviteCollaboratorRejectsNonOwner() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(new User(UUID.randomUUID(), "Owner", "owner@example.com"));

        when(projectRepository.getProjectById(projectId)).thenReturn(project);

        assertThatThrownBy(() -> inviteService.inviteCollaborator(projectId, requesterId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unauthorized: Only the project owner can generate invite links.");

        verify(inviteTokenGeneratorPort, never()).generateToken();
        verify(inviteRepository, never()).saveInvite(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void acceptInviteAddsCollaboratorWhenValid() {
        UUID projectId = UUID.randomUUID();
        UUID acceptingUserId = UUID.randomUUID();
        User acceptingUser = new User(acceptingUserId, "Ada", "ada@example.com");
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(new User(UUID.randomUUID(), "Owner", "owner@example.com"));

        when(inviteRepository.findProjectIdByToken("token")).thenReturn(Optional.of(projectId));
        when(userLookupPort.getUserById(acceptingUserId)).thenReturn(acceptingUser);
        when(projectRepository.getProjectById(projectId)).thenReturn(project);

        inviteService.acceptInvite("token", acceptingUserId);

        verify(managingCollaboratorCase).addCollaborator(projectId, "ada@example.com");
    }

    @Test
    void acceptInviteRejectsInvalidToken() {
        UUID acceptingUserId = UUID.randomUUID();
        when(inviteRepository.findProjectIdByToken("token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.acceptInvite("token", acceptingUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invite link is invalid or has expired.");
    }

    @Test
    void acceptInviteRejectsMissingUser() {
        UUID projectId = UUID.randomUUID();
        UUID acceptingUserId = UUID.randomUUID();

        when(inviteRepository.findProjectIdByToken("token")).thenReturn(Optional.of(projectId));
        when(userLookupPort.getUserById(acceptingUserId)).thenReturn(null);

        assertThatThrownBy(() -> inviteService.acceptInvite("token", acceptingUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found.");

        verify(projectRepository, never()).getProjectById(org.mockito.ArgumentMatchers.any());
        verify(managingCollaboratorCase, never()).addCollaborator(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void acceptInviteRejectsExistingMemberOrOwner() {
        UUID projectId = UUID.randomUUID();
        UUID acceptingUserId = UUID.randomUUID();
        Project project = new Project("Project");
        project.setProjectId(projectId);
        project.setProjectOwner(new User(UUID.randomUUID(), "Owner", "owner@example.com"));
        project.addCollaborator(new User(acceptingUserId, "Ada", "ada@example.com"));

        when(inviteRepository.findProjectIdByToken("token")).thenReturn(Optional.of(projectId));
        when(userLookupPort.getUserById(acceptingUserId)).thenReturn(new User(acceptingUserId, "Ada", "ada@example.com"));
        when(projectRepository.getProjectById(projectId)).thenReturn(project);

        assertThatThrownBy(() -> inviteService.acceptInvite("token", acceptingUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You are already a member of this project.");

        verify(managingCollaboratorCase, never()).addCollaborator(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }
}