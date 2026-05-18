package org.eci.ZwingBackend.project.application.service;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.project.application.port.in.InviteCollaboratorCase;
import org.eci.ZwingBackend.project.application.port.in.ManagingCollaboratorCase;
import org.eci.ZwingBackend.project.application.port.out.InviteRepositoryPort;
import org.eci.ZwingBackend.project.application.port.out.InviteTokenGeneratorPort;
import org.eci.ZwingBackend.project.application.port.out.ProjectRepositoryOutPort;
import org.eci.ZwingBackend.project.application.port.out.UserLookupPort;
import org.eci.ZwingBackend.project.domain.model.Project;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class InviteService implements InviteCollaboratorCase {
    private final ProjectRepositoryOutPort projectRepository;
    private final InviteRepositoryPort inviteRepository;
    private final UserLookupPort userLookupPort;
    private final ManagingCollaboratorCase managingCollaboratorCase;
    private final InviteTokenGeneratorPort inviteTokenGeneratorPort;

    private static final long INVITE_TTL_SECONDS = 15 * 60;

    @Override
    public String inviteCollaborator(UUID projectId, UUID requesterId) {
        Project project = projectRepository.getProjectById(projectId);
        if (!project.getProjectOwner().getUserId().equals(requesterId)) {
            throw new RuntimeException("Unauthorized: Only the project owner can generate invite links.");
        }

        String token = inviteTokenGeneratorPort.generateToken();
        inviteRepository.saveInvite(token, projectId, INVITE_TTL_SECONDS);
        return token;
    }

    @Override
    public void acceptInvite(String inviteToken, UUID acceptingUserId) {
        UUID projectId = inviteRepository.findProjectIdByToken(inviteToken).orElseThrow(() -> new RuntimeException("Invite link is invalid or has expired."));

        User acceptingUser = userLookupPort.getUserById(acceptingUserId);
        if (acceptingUser == null) {
            throw new RuntimeException("User not found.");
        }

        Project project = projectRepository.getProjectById(projectId);
        boolean alreadyMember = project.getCollaborators().stream().anyMatch(c -> c.getUserId().equals(acceptingUserId));
        if (alreadyMember || project.getProjectOwner().getUserId().equals(acceptingUserId)) {
            throw new RuntimeException("You are already a member of this project.");
        }

        managingCollaboratorCase.addCollaborator(projectId, acceptingUser.getEmail());
    }
}