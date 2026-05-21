package org.eci.ZwingBackend.rack.infraestructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.rack.infraestructure.persistence.entity.ChannelEntity;
import org.eci.ZwingBackend.rack.infraestructure.persistence.entity.ChannelRackEntity;
import org.eci.ZwingBackend.rack.infraestructure.persistence.repository.mapper.RackMapper;
import org.eci.ZwingBackend.rack.infraestructure.persistence.postgre.ChannelRackRepository;
import org.eci.ZwingBackend.sound.domain.model.SoundCategory;
import org.eci.ZwingBackend.sound.infrastructure.persistence.entity.SoundPresetEntity;
import org.eci.ZwingBackend.sound.infrastructure.persistence.postgre.SoundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RackPersistenceAdaptersTest {

    @Mock
    private ChannelRackRepository channelRackRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SoundRepository soundRepository;

    @Mock
    private ObjectMapper objectMapper;

    private final RackMapper rackMapper = new RackMapper();

    private RackRepositoryAdapter rackRepositoryAdapter;
    private RedisRackCacheAdapter rackCacheAdapter;
    private SoundLookupAdapter soundLookupAdapter;

    @BeforeEach
    void setUp() {
        rackRepositoryAdapter = new RackRepositoryAdapter(channelRackRepository, rackMapper);
        rackCacheAdapter = new RedisRackCacheAdapter(redisTemplate, objectMapper);
        soundLookupAdapter = new SoundLookupAdapter(soundRepository);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void rackRepositoryAdapterInsertsUpdatesAndReadsRacks() {
        UUID projectId = UUID.randomUUID();
        UUID rackId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID soundId = UUID.randomUUID();

        ChannelRack rack = new ChannelRack(rackId, projectId);
        Channel channel = new Channel(channelId, rackId, "Kick", soundId, 0);
        channel.toggleStep(0);
        rack.setBpm(128);
        rack.addChannel(channel);

        when(channelRackRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(channelRackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChannelRack inserted = rackRepositoryAdapter.save(rack);
        assertThat(inserted.getBpm()).isEqualTo(128);
        assertThat(inserted.getChannels()).hasSize(1);

        ChannelRackEntity existing = new ChannelRackEntity();
        existing.setRackId(rackId);
        existing.setProjectId(projectId);
        existing.setBpm(120);

        ChannelEntity storedChannel = new ChannelEntity();
        storedChannel.setChannelId(UUID.randomUUID());
        storedChannel.setName("Old");
        storedChannel.setSoundId(soundId);
        storedChannel.setActive(true);
        storedChannel.setVolume(1.0f);
        storedChannel.setSteps("false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false");
        storedChannel.setPosition(0);
        storedChannel.setRack(existing);
        existing.getChannels().add(storedChannel);

        when(channelRackRepository.findByProjectId(projectId)).thenReturn(Optional.of(existing));
        when(channelRackRepository.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        ChannelRack updated = rackRepositoryAdapter.save(rack);
        assertThat(updated.getBpm()).isEqualTo(128);
        assertThat(updated.getChannels()).hasSize(1);

        when(channelRackRepository.findByProjectId(projectId)).thenReturn(Optional.of(existing));
        assertThat(rackRepositoryAdapter.findByProjectId(projectId)).isPresent();
        assertThat(rackRepositoryAdapter.getByProjectId(projectId).getRackId()).isEqualTo(rackId);
    }

    @Test
    void rackRepositoryAdapterThrowsWhenRackMissing() {
        UUID projectId = UUID.randomUUID();
        when(channelRackRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rackRepositoryAdapter.getByProjectId(projectId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No rack found");
    }

    @Test
    void rackCacheAdapterCachesReadsAndTracksDirtyState() throws Exception {
        UUID projectId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        rack.setBpm(140);

        when(objectMapper.writeValueAsString(rack)).thenReturn("json");
        when(objectMapper.readValue("json", ChannelRack.class)).thenReturn(rack);
        when(redisTemplate.hasKey("rack_state:" + projectId)).thenReturn(true);
        when(redisTemplate.hasKey("rack_dirty:" + projectId)).thenReturn(true);
        when(redisTemplate.keys("rack_state:*")).thenReturn(Set.of("rack_state:" + projectId, "rack_state:not-a-uuid"));
        when(valueOperations.get("rack_state:" + projectId)).thenReturn("json");

        rackCacheAdapter.cacheRack(projectId, rack);
        assertThat(rackCacheAdapter.getCachedRack(projectId)).contains(rack);
        assertThat(rackCacheAdapter.isCached(projectId)).isTrue();
        assertThat(rackCacheAdapter.getCachedProjectIds()).contains(projectId);

        rackCacheAdapter.markDirty(projectId);
        assertThat(rackCacheAdapter.isDirty(projectId)).isTrue();
        rackCacheAdapter.clearDirty(projectId);
        rackCacheAdapter.evictRack(projectId);

        verify(redisTemplate).delete("rack_state:" + projectId);
        verify(redisTemplate, times(2)).delete("rack_dirty:" + projectId);
    }

    @Test
    void rackCacheAdapterReturnsEmptyAndSkipsInvalidJson() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(valueOperations.get("rack_state:" + projectId)).thenReturn("broken");
        when(objectMapper.readValue("broken", ChannelRack.class)).thenThrow(new JsonProcessingException("bad") {});
        when(redisTemplate.keys("rack_state:*")).thenReturn(Set.of());

        assertThat(rackCacheAdapter.getCachedRack(projectId)).isEmpty();
        assertThat(rackCacheAdapter.getCachedProjectIds()).isEmpty();
    }

    @Test
    void rackCacheAdapterPropagatesSerializationErrors() throws Exception {
        UUID projectId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> rackCacheAdapter.cacheRack(projectId, rack))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to cache rack state");
    }

    @Test
    void soundLookupAdapterFindsCategoryAndExists() {
        UUID soundId = UUID.randomUUID();
        UUID missingSoundId = UUID.randomUUID();
        SoundPresetEntity entity = new SoundPresetEntity();
        entity.setSoundId(soundId);
        entity.setCategory(SoundCategory.KICK);
        when(soundRepository.findById(soundId)).thenReturn(Optional.of(entity));
        when(soundRepository.existsById(soundId)).thenReturn(true);
        when(soundRepository.findById(missingSoundId)).thenReturn(Optional.empty());

        assertThat(soundLookupAdapter.getCategoryBySound(soundId)).isEqualTo("KICK");
        assertThat(soundLookupAdapter.soundExists(soundId)).isTrue();
        assertThatThrownBy(() -> soundLookupAdapter.getCategoryBySound(missingSoundId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sound not found");
    }
}