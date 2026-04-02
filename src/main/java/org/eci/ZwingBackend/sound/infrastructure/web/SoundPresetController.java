package org.eci.ZwingBackend.sound.infrastructure.web;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.eci.ZwingBackend.sound.application.port.in.BrowseSoundsCase;
import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.infrastructure.web.dto.SoundPresetResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sounds")
@AllArgsConstructor
public class SoundPresetController {
    private BrowseSoundsCase browseSoundsCase;

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
}
