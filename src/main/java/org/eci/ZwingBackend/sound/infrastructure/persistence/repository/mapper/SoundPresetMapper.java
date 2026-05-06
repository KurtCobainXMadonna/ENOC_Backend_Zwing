package org.eci.ZwingBackend.sound.infrastructure.persistence.repository.mapper;

import org.eci.ZwingBackend.sound.domain.model.SoundPreset;
import org.eci.ZwingBackend.sound.infrastructure.persistence.entity.SoundPresetEntity;
import org.springframework.stereotype.Component;

@Component
public class SoundPresetMapper {
    public SoundPreset toDomain(SoundPresetEntity entity) {
        return new SoundPreset(
                entity.getSoundId(),
                entity.getName(),
                entity.getCategory(),
                entity.getBlobUrl(),
                entity.getDescription(),
                entity.getProjectId(),
                entity.getUploadedBy()
        );
    }

    public SoundPresetEntity toEntity(SoundPreset domain) {
        SoundPresetEntity entity = new SoundPresetEntity();
        entity.setSoundId(domain.getSoundId());
        entity.setName(domain.getName());
        entity.setCategory(domain.getCategory());
        entity.setBlobUrl(domain.getBlobUrl());
        entity.setDescription(domain.getDescription());
        entity.setProjectId(domain.getProjectId());
        entity.setUploadedBy(domain.getUploadedBy());
        return entity;
    }
}
