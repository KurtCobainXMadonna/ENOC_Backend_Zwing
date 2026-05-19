package org.eci.ZwingBackend.sound.infrastructure.web;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.eci.ZwingBackend.sound.application.port.in.BrowseSoundsCase;
import org.eci.ZwingBackend.sound.application.port.in.DeleteProjectSoundCase;
import org.eci.ZwingBackend.sound.application.port.in.UploadProjectSoundCase;
import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;
import org.eci.ZwingBackend.sound.infrastructure.web.dto.SoundPresetResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sounds")
@AllArgsConstructor
public class SoundPresetController {
    private final BrowseSoundsCase browseSoundsCase;
    private final UploadProjectSoundCase uploadProjectSoundCase;
    private final DeleteProjectSoundCase deleteProjectSoundCase;

    @GetMapping
    public ResponseEntity<GeneralResponse<List<SoundPresetResponse>>> getAllSounds() {
        List<SoundPresetResponse> sounds = browseSoundsCase.getAllSounds().stream().map(SoundPresetResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(GeneralResponse.success(sounds, "Sounds retrieved successfully"));
    }

    @GetMapping(params = "category")
    public ResponseEntity<GeneralResponse<List<SoundPresetResponse>>> getSoundsByCategory(@RequestParam SoundCategory category) {
        List<SoundPresetResponse> sounds = browseSoundsCase.getSoundsByCategory(category).stream().map(SoundPresetResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(GeneralResponse.success(sounds, "Sounds retrieved successfully"));
    }

    @GetMapping("/{soundId}")
    public ResponseEntity<GeneralResponse<SoundPresetResponse>> getSoundById(@PathVariable UUID soundId) {
        SoundPresetResponse sound = SoundPresetResponse.from(browseSoundsCase.getSoundById(soundId));
        return ResponseEntity.ok(GeneralResponse.success(sound, "Sound retrieved successfully"));
    }

    /** Globals + uploads scoped to the project. Requester must be owner or collaborator. */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<GeneralResponse<List<SoundPresetResponse>>> getSoundsForProject(@PathVariable UUID projectId, @RequestHeader("X-User-Id") UUID requesterId) {
        List<SoundPresetResponse> sounds = browseSoundsCase.getSoundsForProject(projectId, requesterId).stream().map(SoundPresetResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(GeneralResponse.success(sounds, "Project sounds retrieved successfully"));
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GeneralResponse<SoundPresetResponse>> uploadProjectSound(@RequestPart("file") MultipartFile file, @RequestParam("projectId") UUID projectId, @RequestParam("name") String name, @RequestParam("category") SoundCategory category, @RequestParam(value = "description", required = false) String description, @RequestHeader("X-User-Id") UUID requesterId) throws IOException {
        UploadProjectSoundCase.UploadCommand command = new UploadProjectSoundCase.UploadCommand(
                projectId,
                requesterId,
                name,
                category,
                description,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        );

        SoundPreset uploaded = uploadProjectSoundCase.uploadProjectSound(command);
        return ResponseEntity.ok(GeneralResponse.success(SoundPresetResponse.from(uploaded), "Sound uploaded successfully"));
    }

    @DeleteMapping("/{soundId}")
    public ResponseEntity<GeneralResponse<Void>> deleteProjectSound(@PathVariable UUID soundId, @RequestParam("projectId") UUID projectId, @RequestHeader("X-User-Id") UUID requesterId) {
        deleteProjectSoundCase.deleteProjectSound(soundId, projectId, requesterId);
        return ResponseEntity.ok(GeneralResponse.success(null, "Sound deleted successfully"));
    }
}
