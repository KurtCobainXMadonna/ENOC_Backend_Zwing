package org.eci.ZwingBackend.sound.application.port.in;

import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;

import java.util.List;
import java.util.UUID;

public interface BrowseSoundsCase {
    List<SoundPreset> getAllSounds();
    List<SoundPreset> getSoundsByCategory(SoundCategory category);
    SoundPreset getSoundById(UUID soundId);
}