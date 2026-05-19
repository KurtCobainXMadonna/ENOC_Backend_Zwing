package org.eci.ZwingBackend.presence.application.port.in;

import org.eci.ZwingBackend.presence.domain.model.Presence;

import java.util.List;
import java.util.UUID;


public interface ManagePresenceCase {
    Presence userJoined(UUID projectId, String userId, String email, String displayName);
    boolean userLeft(UUID projectId, String userId);

    List<Presence> getRoster(UUID projectId);
    long getUserCount(UUID projectId);
}
