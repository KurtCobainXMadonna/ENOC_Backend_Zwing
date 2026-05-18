package org.eci.ZwingBackend.presence.application.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.presence.application.port.in.ManagePresenceCase;
import org.eci.ZwingBackend.presence.application.port.out.PresenceStorePort;
import org.eci.ZwingBackend.presence.domain.model.Presence;
import org.eci.ZwingBackend.presence.domain.model.UserColor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@AllArgsConstructor
public class PresenceService implements ManagePresenceCase {
    private final PresenceStorePort store;

    private static final List<String> PALETTE = UserColor.allHex();

    @Override
    public Presence userJoined(UUID projectId, String userId, String email, String displayName) {
        // Reconnect case: user was already present, keep their color.
        Optional<Presence> existing = store.findPresence(projectId, userId);
        if (existing.isPresent()) {
            log.info("[Presence] User {} reconnected to project {} — keeping color {}.", userId, projectId, existing.get().getColor());
            return existing.get();
        }

        String color = store.claimColor(projectId, PALETTE).orElseGet(() -> fallbackColor(userId));

        Presence presence = new Presence(
                projectId,
                userId,
                email,
                displayName != null ? displayName : email,
                color,
                Instant.now()
        );
        store.savePresence(presence);

        log.info("[Presence] User {} ({}) joined project {} with color {}. Total now: {}", userId, email, projectId, color, store.countInProject(projectId));
        return presence;
    }

    @Override
    public boolean userLeft(UUID projectId, String userId) {
        Optional<Presence> existing = store.findPresence(projectId, userId);
        if (existing.isEmpty()) {
            log.debug("[Presence] userLeft called for user {} in project {} but no presence entry exists — skipping.", userId, projectId);
            return store.countInProject(projectId) == 0;
        }

        Presence presence = existing.get();
        store.removePresence(projectId, userId);

        if (PALETTE.contains(presence.getColor())) {
            store.releaseColor(projectId, presence.getColor());
        }

        long remaining = store.countInProject(projectId);
        log.info("[Presence] User {} left project {}. {} user(s) remaining.", userId, projectId, remaining);
        return remaining == 0;
    }

    @Override
    public List<Presence> getRoster(UUID projectId) {
        return store.findAllInProject(projectId);
    }

    @Override
    public long getUserCount(UUID projectId) {
        return store.countInProject(projectId);
    }

    private String fallbackColor(String userId) {
        int idx = Math.floorMod(userId.hashCode(), PALETTE.size());
        String color = PALETTE.get(idx);
        log.warn("[Presence] Color palette exhausted for user {} — falling back to hashed color {}.", userId, color);
        return color;
    }
}
