package org.eci.ZwingBackend.project.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface InviteRepositoryPort {
    void saveInvite(String token, UUID projectId, long ttlSeconds);
    Optional<UUID> findProjectIdByToken(String token);
    void deleteInvite(String token);
}