package org.eci.ZwingBackend.sound.infrastructure.persistence.postgre;

import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.infrastructure.persistence.entity.SoundPresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SoundRepository extends JpaRepository<SoundPresetEntity, UUID> {
    List<SoundPresetEntity> findByCategory(SoundCategory category);
}