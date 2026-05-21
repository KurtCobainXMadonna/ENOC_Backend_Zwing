package org.eci.ZwingBackend.sound.application.service;

import org.eci.ZwingBackend.project.application.port.in.ManagingProjectsCase;
import org.eci.ZwingBackend.sound.application.port.in.UploadProjectSoundCase.UploadCommand;
import org.eci.ZwingBackend.sound.application.port.out.AudioStoragePort;
import org.eci.ZwingBackend.sound.application.port.out.SoundRepositoryPort;
import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoundServiceTest {

    @Mock
    private SoundRepositoryPort soundRepository;

    @Mock
    private AudioStoragePort audioStorage;

    @Mock
    private ManagingProjectsCase managingProjectsCase;

    private SoundService soundService;

    @BeforeEach
    void setUp() {
        soundService = new SoundService(soundRepository, audioStorage, managingProjectsCase);
    }

    @Test
    void browseMethodsDelegateToRepository() {
        SoundPreset preset = new SoundPreset(UUID.randomUUID(), "Kick", SoundCategory.KICK, "url", "desc", null, null);
        when(soundRepository.findAllGlobal()).thenReturn(List.of(preset));
        when(soundRepository.findGlobalByCategory(SoundCategory.KICK)).thenReturn(List.of(preset));

        assertThat(soundService.getAllSounds()).containsExactly(preset);
        assertThat(soundService.getSoundsByCategory(SoundCategory.KICK)).containsExactly(preset);
    }

    @Test
    void getSoundByIdFailsWhenMissing() {
        UUID soundId = UUID.randomUUID();
        when(soundRepository.findById(soundId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> soundService.getSoundById(soundId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Sound not found: " + soundId);
    }

    @Test
    void getSoundsForProjectChecksMembership() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        SoundPreset preset = new SoundPreset(UUID.randomUUID(), "Kick", SoundCategory.KICK, "url", "desc", projectId, requesterId);

        when(soundRepository.findVisibleToProject(projectId)).thenReturn(List.of(preset));

        assertThat(soundService.getSoundsForProject(projectId, requesterId)).containsExactly(preset);
        verify(managingProjectsCase).getProjectById(projectId, requesterId);
    }

    @Test
    void uploadProjectSoundValidatesAndPersists() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(managingProjectsCase.getProjectById(projectId, requesterId)).thenReturn(null);

        UploadCommand command = new UploadCommand(
                projectId,
                requesterId,
                "Kick",
                SoundCategory.KICK,
                "desc",
                "kick.WAV",
                "audio/wav",
                128,
                new ByteArrayInputStream(new byte[] {1, 2, 3})
        );
        when(audioStorage.upload(any(), any(), any(Long.class), any())).thenReturn("https://cdn.example.com/sound.wav");
        when(soundRepository.save(any(SoundPreset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SoundPreset saved = soundService.uploadProjectSound(command);

        ArgumentCaptor<String> blobPathCaptor = ArgumentCaptor.forClass(String.class);
        verify(audioStorage).upload(blobPathCaptor.capture(), any(), any(Long.class), any());
        assertThat(blobPathCaptor.getValue()).endsWith(".wav");
        assertThat(saved.getBlobUrl()).isEqualTo("https://cdn.example.com/sound.wav");
        assertThat(saved.getProjectId()).isEqualTo(projectId);
        assertThat(saved.getUploadedBy()).isEqualTo(requesterId);
    }

    @Test
    void uploadProjectSoundRejectsUnsupportedFormat() {
        UploadCommand command = new UploadCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Kick",
                SoundCategory.KICK,
                "desc",
                "kick.bin",
                "application/octet-stream",
                128,
                new ByteArrayInputStream(new byte[] {1})
        );

        assertThatThrownBy(() -> soundService.uploadProjectSound(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported audio format");
        verify(audioStorage, never()).upload(any(), any(), any(Long.class), any());
    }

    @Test
    void uploadProjectSoundRejectsEmptyFile() {
        UploadCommand command = new UploadCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Kick",
                SoundCategory.KICK,
                "desc",
                "kick.wav",
                "audio/wav",
                0,
                new ByteArrayInputStream(new byte[0])
        );

        assertThatThrownBy(() -> soundService.uploadProjectSound(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file is empty.");
    }

    @Test
    void deleteProjectSoundRejectsGlobalPreset() {
        UUID soundId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        SoundPreset preset = new SoundPreset(soundId, "Kick", SoundCategory.KICK, "url", "desc", null, null);
        when(soundRepository.findById(soundId)).thenReturn(Optional.of(preset));

        assertThatThrownBy(() -> soundService.deleteProjectSound(soundId, projectId, requesterId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Global presets cannot be deleted.");
    }

    @Test
    void deleteProjectSoundRemovesRowAndBlob() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID soundId = UUID.randomUUID();
        SoundPreset preset = new SoundPreset(soundId, "Kick", SoundCategory.KICK, "https://blob", "desc", projectId, requesterId);
        when(soundRepository.findById(soundId)).thenReturn(Optional.of(preset));
        when(managingProjectsCase.getProjectById(projectId, requesterId)).thenReturn(null);

        soundService.deleteProjectSound(soundId, projectId, requesterId);

        verify(soundRepository).deleteById(soundId);
        verify(audioStorage).deleteByUrl("https://blob");
    }

    @Test
    void deleteProjectSoundSwallowsBlobErrors() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID soundId = UUID.randomUUID();
        SoundPreset preset = new SoundPreset(soundId, "Kick", SoundCategory.KICK, "https://blob", "desc", projectId, requesterId);
        when(soundRepository.findById(soundId)).thenReturn(Optional.of(preset));
        when(managingProjectsCase.getProjectById(projectId, requesterId)).thenReturn(null);
        doThrow(new RuntimeException("boom")).when(audioStorage).deleteByUrl("https://blob");

        soundService.deleteProjectSound(soundId, projectId, requesterId);

        verify(soundRepository).deleteById(soundId);
    }
}