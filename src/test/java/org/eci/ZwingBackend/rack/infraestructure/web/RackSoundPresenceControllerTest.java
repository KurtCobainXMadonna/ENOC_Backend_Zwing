package org.eci.ZwingBackend.rack.infraestructure.web;

import org.eci.ZwingBackend.presence.application.port.in.ManagePresenceCase;
import org.eci.ZwingBackend.presence.domain.model.Presence;
import org.eci.ZwingBackend.presence.infrastructure.web.PresenceController;
import org.eci.ZwingBackend.rack.application.port.in.ManageChannelCase;
import org.eci.ZwingBackend.rack.application.port.in.ManageRackCase;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.rack.infraestructure.web.dto.request.AddChannelRequest;
import org.eci.ZwingBackend.rack.infraestructure.web.dto.request.UpdateChannelRequest;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.eci.ZwingBackend.sound.application.port.in.BrowseSoundsCase;
import org.eci.ZwingBackend.sound.application.port.in.DeleteProjectSoundCase;
import org.eci.ZwingBackend.sound.application.port.in.UploadProjectSoundCase;
import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;
import org.eci.ZwingBackend.sound.infrastructure.web.SoundPresetController;
import org.eci.ZwingBackend.sound.infrastructure.web.dto.SoundPresetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RackSoundPresenceControllerTest {

    @Mock
    private ManageRackCase manageRackCase;

    @Mock
    private ManageChannelCase manageChannelCase;

    @Mock
    private BrowseSoundsCase browseSoundsCase;

    @Mock
    private UploadProjectSoundCase uploadProjectSoundCase;

    @Mock
    private DeleteProjectSoundCase deleteProjectSoundCase;

    @Mock
    private ManagePresenceCase presenceCase;

    private RackController rackController;
    private SoundPresetController soundController;
    private PresenceController presenceController;

    @BeforeEach
    void setUp() {
        rackController = new RackController(manageRackCase, manageChannelCase);
        soundController = new SoundPresetController(browseSoundsCase, uploadProjectSoundCase, deleteProjectSoundCase);
        presenceController = new PresenceController(presenceCase);
    }

    @Test
    void rackControllerReturnsRackAndDelegatesMutations() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        Channel channel = new Channel(UUID.randomUUID(), rack.getRackId(), "Kick", UUID.randomUUID(), 0);
        when(manageRackCase.getRackByProject(projectId)).thenReturn(rack);
        when(manageChannelCase.addChannel(any(), any(), any(), any())).thenReturn(channel);
        when(manageChannelCase.updateChannel(any(), any(), any(), any(), any(Float.class), any(Boolean.class), any())).thenReturn(channel);

        var getEntity = rackController.getRack(projectId, requesterId);
        var addEntity = rackController.addChannel(projectId, addChannelRequest(), requesterId);
        var updateEntity = rackController.updateChannel(projectId, channel.getChannelId(), updateChannelRequest(), requesterId);
        var deleteEntity = rackController.removeChannel(projectId, channel.getChannelId(), requesterId);

        assertThat(getEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(manageRackCase).getRackByProject(projectId);
        verify(manageChannelCase).removeChannel(projectId, channel.getChannelId(), requesterId);
    }

    @Test
    void soundControllerCoversBrowseUploadAndDelete() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        SoundPreset preset = new SoundPreset(UUID.randomUUID(), "Kick", SoundCategory.KICK, "url", "desc", projectId, requesterId);
        when(browseSoundsCase.getAllSounds()).thenReturn(List.of(preset));
        when(browseSoundsCase.getSoundsByCategory(SoundCategory.KICK)).thenReturn(List.of(preset));
        when(browseSoundsCase.getSoundById(preset.getSoundId())).thenReturn(preset);
        when(browseSoundsCase.getSoundsForProject(projectId, requesterId)).thenReturn(List.of(preset));
        when(uploadProjectSoundCase.uploadProjectSound(any())).thenReturn(preset);

        ResponseEntity<GeneralResponse<List<SoundPresetResponse>>> all = soundController.getAllSounds();
        ResponseEntity<GeneralResponse<List<SoundPresetResponse>>> byCategory = soundController.getSoundsByCategory(SoundCategory.KICK);
        ResponseEntity<GeneralResponse<SoundPresetResponse>> byId = soundController.getSoundById(preset.getSoundId());
        ResponseEntity<GeneralResponse<List<SoundPresetResponse>>> forProject = soundController.getSoundsForProject(projectId, requesterId);
        ResponseEntity<GeneralResponse<SoundPresetResponse>> uploaded = soundController.uploadProjectSound(new MockMultipartFile("file", "kick.wav", "audio/wav", new byte[] {1, 2, 3}), projectId, "Kick", SoundCategory.KICK, "desc", requesterId);
        ResponseEntity<GeneralResponse<Void>> deleted = soundController.deleteProjectSound(preset.getSoundId(), projectId, requesterId);

        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(byCategory.getBody().getData()).hasSize(1);
        assertThat(byId.getBody().getData().getName()).isEqualTo("Kick");
        assertThat(forProject.getBody().getData()).hasSize(1);
        assertThat(uploaded.getBody().getData().getSoundId()).isEqualTo(preset.getSoundId());
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void presenceControllerReturnsRoster() {
        UUID projectId = UUID.randomUUID();
        Presence presence = new Presence(projectId, "user", "user@example.com", "User", "#fff", java.time.Instant.now());
        when(presenceCase.getRoster(projectId)).thenReturn(List.of(presence));

        ResponseEntity<GeneralResponse<List<Presence>>> entity = presenceController.getRoster(projectId, "user");

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getData()).containsExactly(presence);
    }

    private AddChannelRequest addChannelRequest() {
        AddChannelRequest request = new AddChannelRequest();
        request.setName("Kick");
        request.setSoundId(UUID.randomUUID());
        return request;
    }

    private UpdateChannelRequest updateChannelRequest() {
        UpdateChannelRequest request = new UpdateChannelRequest();
        request.setName("Kick 2");
        request.setSoundId(UUID.randomUUID());
        request.setVolume(0.75f);
        request.setActive(true);
        return request;
    }
}