package org.eci.ZwingBackend.project.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface InviteRepositoryPort {
    void saveInvite(String token, UUID projectId, String inviteeEmail, long ttlSeconds);
    Optional<InviteData> findInvite(String token);
    void deleteInvite(String token);

    record InviteData(UUID projectId, String inviteeEmail) {}
}