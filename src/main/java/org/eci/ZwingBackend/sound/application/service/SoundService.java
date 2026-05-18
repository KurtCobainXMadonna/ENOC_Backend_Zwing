package org.eci.ZwingBackend.sound.application.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.project.application.port.in.ManagingProjectsCase;
import org.eci.ZwingBackend.sound.application.port.in.BrowseSoundsCase;
import org.eci.ZwingBackend.sound.application.port.in.DeleteProjectSoundCase;
import org.eci.ZwingBackend.sound.application.port.in.UploadProjectSoundCase;
import org.eci.ZwingBackend.sound.application.port.out.AudioStoragePort;
import org.eci.ZwingBackend.sound.application.port.out.SoundRepositoryPort;
import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class SoundService implements BrowseSoundsCase, UploadProjectSoundCase, DeleteProjectSoundCase {
    private final SoundRepositoryPort soundRepository;
    private final AudioStoragePort audioStorage;
    private final ManagingProjectsCase managingProjectsCase;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "audio/wav", "audio/wave", "audio/x-wav",
            "audio/mpeg", "audio/mp3",
            "audio/ogg",
            "audio/mp4", "audio/aac", "audio/x-m4a",
            "audio/flac",
            "audio/webm",
            "video/mp4"
    );
    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;

    // ── Browse ────────────────────────────────────────────────────────────────

    @Override
    public List<SoundPreset> getAllSounds() {
        return soundRepository.findAllGlobal();
    }

    @Override
    public List<SoundPreset> getSoundsByCategory(SoundCategory category) {
        return soundRepository.findGlobalByCategory(category);
    }

    @Override
    public SoundPreset getSoundById(UUID soundId) {
        return soundRepository.findById(soundId)
                .orElseThrow(() -> new RuntimeException("Sound not found: " + soundId));
    }

    @Override
    public List<SoundPreset> getSoundsForProject(UUID projectId, UUID requesterId) {
        // Throws if requester is not owner/collaborator.
        managingProjectsCase.getProjectById(projectId, requesterId);
        return soundRepository.findVisibleToProject(projectId);
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SoundPreset uploadProjectSound(UploadCommand cmd) {
        validateUpload(cmd);
        // Membership check — throws if requester is not a member.
        managingProjectsCase.getProjectById(cmd.projectId(), cmd.requesterId());

        UUID soundId = UUID.randomUUID();
        String extension = resolveExtension(cmd.originalFilename(), cmd.contentType());
        String blobPath = cmd.projectId() + "/" + soundId + extension;

        String blobUrl = audioStorage.upload(blobPath, cmd.data(), cmd.size(), cmd.contentType());

        SoundPreset preset = new SoundPreset(
                soundId,
                cmd.name(),
                cmd.category(),
                blobUrl,
                cmd.description(),
                cmd.projectId(),
                cmd.requesterId()
        );
        SoundPreset saved = soundRepository.save(preset);
        log.info("[SoundUpload] Project {} got new sound {} ('{}') uploaded by {}.",
                cmd.projectId(), soundId, cmd.name(), cmd.requesterId());
        return saved;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteProjectSound(UUID soundId, UUID projectId, UUID requesterId) {
        SoundPreset preset = soundRepository.findById(soundId)
                .orElseThrow(() -> new RuntimeException("Sound not found: " + soundId));

        if (preset.isGlobal()) {
            throw new RuntimeException("Global presets cannot be deleted.");
        }
        if (!preset.getProjectId().equals(projectId)) {
            throw new RuntimeException("Sound does not belong to the specified project.");
        }
        // Member-only — owner OR collaborator. Throws otherwise.
        managingProjectsCase.getProjectById(projectId, requesterId);

        soundRepository.deleteById(soundId);
        try {
            audioStorage.deleteByUrl(preset.getBlobUrl());
        } catch (Exception e) {
            // The DB row is the source of truth; a leaked blob can be reaped later.
            log.error("[SoundUpload] Row deleted but blob removal failed for {}: {}", preset.getBlobUrl(), e.getMessage());
        }
        log.info("[SoundUpload] Sound {} deleted from project {} by {}.", soundId, projectId, requesterId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateUpload(UploadCommand cmd) {
        if (cmd.name() == null || cmd.name().isBlank()) {
            throw new IllegalArgumentException("Sound name is required.");
        }
        if (cmd.category() == null) {
            throw new IllegalArgumentException("Sound category is required.");
        }
        if (cmd.size() <= 0) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        if (cmd.size() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("Uploaded file exceeds 10MB limit.");
        }
        String contentType = cmd.contentType() == null ? "" : cmd.contentType().toLowerCase();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported audio format: " + cmd.contentType());
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dot).toLowerCase();
                if (ext.matches("\\.[a-z0-9]{1,5}")) {
                    return ext;
                }
            }
        }
        return switch (contentType == null ? "" : contentType.toLowerCase()) {
            case "audio/wav", "audio/wave", "audio/x-wav" -> ".wav";
            case "audio/mpeg", "audio/mp3" -> ".mp3";
            case "audio/ogg" -> ".ogg";
            case "audio/mp4", "video/mp4", "audio/x-m4a" -> ".mp4";
            case "audio/aac" -> ".aac";
            case "audio/flac" -> ".flac";
            case "audio/webm" -> ".webm";
            default -> ".bin";
        };
    }
}
