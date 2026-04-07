package org.eci.ZwingBackend.rack.application.port.out;

import java.util.UUID;

public interface SoundLookupPort {
    String getCategoryBySound(UUID soundId);
    boolean soundExists(UUID soundId);
}
