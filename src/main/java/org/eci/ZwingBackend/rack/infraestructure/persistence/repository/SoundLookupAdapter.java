package org.eci.ZwingBackend.rack.infraestructure.persistence.repository;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.port.out.SoundLookupPort;
import org.eci.ZwingBackend.sound.infrastructure.persistence.postgre.SoundRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class SoundLookupAdapter implements SoundLookupPort {
    private final SoundRepository soundRepository;

    @Override
    public String getCategoryBySound(UUID soundId) {
        return soundRepository.findById(soundId)
                .map(entity -> entity.getCategory().name())
                .orElseThrow(() -> new RuntimeException("Sound not found: " + soundId));
    }

    @Override
    public boolean soundExists(UUID soundId) {
        return soundRepository.existsById(soundId);
    }
}
