package org.eci.ZwingBackend.sound.application.service;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.sound.application.port.in.BrowseSoundsCase;
import org.eci.ZwingBackend.sound.application.port.out.SoundRepositoryPort;
import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class SoundService implements BrowseSoundsCase {
    private final SoundRepositoryPort soundRepository;

    @Override
    public List<SoundPreset> getAllSounds() {
        return soundRepository.findAll();
    }

    @Override
    public List<SoundPreset> getSoundsByCategory(SoundCategory category) {
        return soundRepository.findByCategory(category);
    }

    @Override
    public SoundPreset getSoundById(UUID soundId) {
        return soundRepository.findById(soundId).orElseThrow(() -> new RuntimeException("Sound not found: " + soundId));
    }
}