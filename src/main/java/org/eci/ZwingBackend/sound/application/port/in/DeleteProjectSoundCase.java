package org.eci.ZwingBackend.sound.application.port.in;

import java.util.UUID;

public interface DeleteProjectSoundCase {
    void deleteProjectSound(UUID soundId, UUID projectId, UUID requesterId);
}
