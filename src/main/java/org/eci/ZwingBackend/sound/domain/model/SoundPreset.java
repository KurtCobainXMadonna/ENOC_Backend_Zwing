package org.eci.ZwingBackend.sound.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class SoundPreset {
    private UUID soundId;
    private String name;          // Ex: "Kick 808", "Snare Crack"
    private SoundCategory category;
    private String blobUrl;       // Azure CDN URL — frontend fetches audio from here
    private String description;

    private UUID projectId;   // Null for global presets
    private UUID uploadedBy;  // Null for global presets

    public boolean isGlobal() {
        return projectId == null;
    }
}