package org.eci.ZwingBackend.sound.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.eci.ZwingBackend.sound.domain.model.SoundCategory;

import java.util.UUID;

@Entity
@Data
@Table(name = "sound_presets")
public class SoundPresetEntity {

    @Id
    private UUID soundId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SoundCategory category;

    @Column(nullable = false)
    private String blobUrl;

    private String description;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;
}
