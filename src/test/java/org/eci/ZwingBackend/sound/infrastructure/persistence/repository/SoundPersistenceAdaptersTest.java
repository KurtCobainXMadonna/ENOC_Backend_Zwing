package org.eci.ZwingBackend.sound.infrastructure.persistence.repository;

import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.domain.model.SoundPreset;
import org.eci.ZwingBackend.sound.infrastructure.persistence.entity.SoundPresetEntity;
import org.eci.ZwingBackend.sound.infrastructure.persistence.postgre.SoundRepository;
import org.eci.ZwingBackend.sound.infrastructure.persistence.repository.mapper.SoundPresetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoundPersistenceAdaptersTest {

    @Mock
    private SoundRepository soundRepository;

    private final SoundPresetMapper mapper = new SoundPresetMapper();
    private SoundRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SoundRepositoryAdapter(soundRepository, mapper);
    }

    @Test
    void soundRepositoryAdapterMapsFindersAndSave() {
        UUID soundId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        SoundPresetEntity entity = new SoundPresetEntity();
        entity.setSoundId(soundId);
        entity.setName("Kick");
        entity.setCategory(SoundCategory.KICK);
        entity.setBlobUrl("https://cdn/kick.wav");
        entity.setDescription("desc");
        entity.setProjectId(projectId);
        entity.setUploadedBy(UUID.randomUUID());

        when(soundRepository.findByProjectIdIsNull()).thenReturn(List.of(entity));
        when(soundRepository.findByProjectIdIsNullAndCategory(SoundCategory.KICK)).thenReturn(List.of(entity));
        when(soundRepository.findByProjectIdIsNullOrProjectId(projectId)).thenReturn(List.of(entity));
        when(soundRepository.findById(soundId)).thenReturn(Optional.of(entity));
        when(soundRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SoundPreset preset = mapper.toDomain(entity);
        assertThat(adapter.findAllGlobal()).hasSize(1);
        assertThat(adapter.findGlobalByCategory(SoundCategory.KICK)).hasSize(1);
        assertThat(adapter.findVisibleToProject(projectId)).hasSize(1);
        assertThat(adapter.findById(soundId)).contains(preset);
        assertThat(adapter.save(preset)).usingRecursiveComparison().isEqualTo(preset);

        adapter.deleteById(soundId);
        verify(soundRepository).deleteById(soundId);
    }

    @Test
    void soundRepositoryAdapterReturnsEmptyWhenMissing() {
        when(soundRepository.findById(any())).thenReturn(Optional.empty());

        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }
}