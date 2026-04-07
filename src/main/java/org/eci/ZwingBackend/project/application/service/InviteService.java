package org.eci.ZwingBackend.project.application.service;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.project.application.port.in.InviteCollaboratorCase;
import org.eci.ZwingBackend.project.application.port.in.ManagingCollaboratorCase;
import org.eci.ZwingBackend.project.application.port.out.InviteRepositoryPort;
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

    private static final long INVITE_TTL_SECONDS = 48 * 60 * 60;

    @Override
    public String inviteCollaborator(UUID projectId, String inviteeEmail, UUID requesterId) {
        Project project = projectRepository.getProjectById(projectId);
        if (!project.getProjectOwner().getUserId().equals(requesterId)) {
            throw new RuntimeException("Unauthorized: Only the project owner can invite collaborators.");
        }

        User invitee = userLookupPort.getUserByEmail(inviteeEmail);
        if (invitee == null) {
            throw new RuntimeException("No Zwing account found for: " + inviteeEmail);
        }

        boolean alreadyMember = project.getCollaborators().stream().anyMatch(c -> c.getEmail().equals(inviteeEmail));
        if (alreadyMember || project.getProjectOwner().getEmail().equals(inviteeEmail)) {
            throw new RuntimeException("User is already a member of this project.");
        }

        String token = UUID.randomUUID().toString();
        inviteRepository.saveInvite(token, projectId, inviteeEmail, INVITE_TTL_SECONDS);
        return token;
    }

    @Override
    public void acceptInvite(String inviteToken, UUID acceptingUserId) {
        InviteRepositoryPort.InviteData invite = inviteRepository.findInvite(inviteToken).orElseThrow(() -> new RuntimeException("Invite not found or has expired."));

        User acceptingUser = userLookupPort.getUserById(acceptingUserId);
        if (acceptingUser == null) {
            throw new RuntimeException("User not found.");
        }

        if (!acceptingUser.getEmail().equals(invite.inviteeEmail())) {
            throw new RuntimeException("This invite was sent to a different email address.");
        }

        managingCollaboratorCase.addCollaborator(invite.projectId(), invite.inviteeEmail());
        inviteRepository.deleteInvite(inviteToken);
    }
}