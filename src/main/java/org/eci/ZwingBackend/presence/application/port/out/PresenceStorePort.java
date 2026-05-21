package org.eci.ZwingBackend.presence.application.port.out;

import org.eci.ZwingBackend.presence.domain.model.Presence;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PresenceStorePort {
    void savePresence(Presence presence);
    void removePresence(UUID projectId, String userId);

    Optional<Presence> findPresence(UUID projectId, String userId);
    List<Presence> findAllInProject(UUID projectId);
    long countInProject(UUID projectId);

    // ── Color pool management ─────────────────────────────────────────────────
    Optional<String> claimColor(UUID projectId, List<String> palette);
    void releaseColor(UUID projectId, String colorHex);
    void clearProject(UUID projectId);
}
