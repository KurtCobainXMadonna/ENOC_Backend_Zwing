package org.eci.ZwingBackend.sound.application.port.out;

import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SoundRepositoryPort {
    List<SoundPreset> findAll();
    List<SoundPreset> findByCategory(SoundCategory category);
    Optional<SoundPreset> findById(UUID soundId);
    SoundPreset save(SoundPreset soundPreset);
}