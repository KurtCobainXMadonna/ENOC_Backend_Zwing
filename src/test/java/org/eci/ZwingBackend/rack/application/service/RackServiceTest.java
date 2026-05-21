package org.eci.ZwingBackend.rack.application.service;

import org.eci.ZwingBackend.rack.application.port.out.RackCachePort;
import org.eci.ZwingBackend.rack.application.port.out.RackRepositoryPort;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RackServiceTest {

    @Mock
    private RackRepositoryPort rackRepository;

    @Mock
    private RackCachePort rackCache;

    private RackService rackService;

    @BeforeEach
    void setUp() {
        rackService = new RackService(rackRepository, rackCache);
    }

    @Test
    void createRackForProjectCreatesAndCachesNewRack() {
        UUID projectId = UUID.randomUUID();
        ChannelRack createdRack = new ChannelRack(UUID.randomUUID(), projectId);

        when(rackRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(rackRepository.save(org.mockito.ArgumentMatchers.any(ChannelRack.class))).thenReturn(createdRack);

        ChannelRack rack = rackService.createRackForProject(projectId);

        assertThat(rack).isSameAs(createdRack);
        verify(rackCache).cacheRack(projectId, createdRack);
    }

    @Test
    void createRackForProjectReturnsExistingRackWithoutSaving() {
        UUID projectId = UUID.randomUUID();
        ChannelRack existing = new ChannelRack(UUID.randomUUID(), projectId);

        when(rackRepository.findByProjectId(projectId)).thenReturn(Optional.of(existing));

        ChannelRack rack = rackService.createRackForProject(projectId);

        assertThat(rack).isSameAs(existing);
        verify(rackRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(rackCache, never()).cacheRack(projectId, existing);
    }

    @Test
    void getRackByProjectUsesCacheWhenAvailable() {
        UUID projectId = UUID.randomUUID();
        ChannelRack cached = new ChannelRack(UUID.randomUUID(), projectId);
        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.of(cached));

        ChannelRack rack = rackService.getRackByProject(projectId);

        assertThat(rack).isSameAs(cached);
        verify(rackRepository, never()).getByProjectId(projectId);
    }

    @Test
    void getRackByProjectLoadsFromDatabaseOnCacheMiss() {
        UUID projectId = UUID.randomUUID();
        ChannelRack fromDb = new ChannelRack(UUID.randomUUID(), projectId);
        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.empty());
        when(rackRepository.getByProjectId(projectId)).thenReturn(fromDb);

        ChannelRack rack = rackService.getRackByProject(projectId);

        assertThat(rack).isSameAs(fromDb);
        verify(rackCache).cacheRack(projectId, fromDb);
    }

    @Test
    void updateBpmClampsToAllowedRange() {
        UUID projectId = UUID.randomUUID();
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
        when(rackCache.getCachedRack(projectId)).thenReturn(Optional.of(rack));

        ChannelRack updated = rackService.updateBpm(projectId, 500);

        assertThat(updated.getBpm()).isEqualTo(240);
        verify(rackCache).cacheRack(projectId, rack);
    }
}