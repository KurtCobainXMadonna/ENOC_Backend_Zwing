package org.eci.ZwingBackend.project.application.port.in;

import java.util.UUID;

public interface InviteCollaboratorCase {
    String inviteCollaborator(UUID projectId, UUID requesterId);
    void acceptInvite(String inviteToken, UUID acceptingUserId);
}