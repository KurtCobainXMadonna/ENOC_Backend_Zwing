package org.eci.ZwingBackend.sound.application.port.in;

import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;

import java.io.InputStream;
import java.util.UUID;

public interface UploadProjectSoundCase {
    SoundPreset uploadProjectSound(UploadCommand command);

    record UploadCommand(
            UUID projectId,
            UUID requesterId,
            String name,
            SoundCategory category,
            String description,
            String originalFilename,
            String contentType,
            long size,
            InputStream data
    ) {}
}
