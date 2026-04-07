package org.eci.ZwingBackend.sound.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;

import java.util.UUID;

@Data
@AllArgsConstructor
public class SoundPresetResponse {
    private UUID soundId;
    private String name;
    private SoundCategory category;
    private String blobUrl;
    private String description;

    public static SoundPresetResponse from(SoundPreset preset) {
        return new SoundPresetResponse(
                preset.getSoundId(),
                preset.getName(),
                preset.getCategory(),
                preset.getBlobUrl(),
                preset.getDescription()
        );
    }
}