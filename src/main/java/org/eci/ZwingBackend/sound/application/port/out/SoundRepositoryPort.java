package org.eci.ZwingBackend.sound.application.port.out;

import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SoundRepositoryPort {
    List<SoundPreset> findAllGlobal();
    List<SoundPreset> findGlobalByCategory(SoundCategory category);
    List<SoundPreset> findVisibleToProject(UUID projectId);
    Optional<SoundPreset> findById(UUID soundId);
    SoundPreset save(SoundPreset soundPreset);
    void deleteById(UUID soundId);
}
