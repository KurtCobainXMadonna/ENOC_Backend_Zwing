package org.eci.ZwingBackend.rack.infraestructure.persistence.repository;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.port.out.RackRepositoryPort;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.rack.infraestructure.persistence.entity.ChannelEntity;
import org.eci.ZwingBackend.rack.infraestructure.persistence.entity.ChannelRackEntity;
import org.eci.ZwingBackend.rack.infraestructure.persistence.postgre.ChannelRackRepository;
import org.eci.ZwingBackend.rack.infraestructure.persistence.repository.mapper.RackMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


@Component
@AllArgsConstructor
public class RackRepositoryAdapter implements RackRepositoryPort {
    private final ChannelRackRepository jpaRepository;
    private final RackMapper mapper;

    @Override
    @Transactional
    public ChannelRack save(ChannelRack rack) {
        // Check if a rack already exists in the DB
        Optional<ChannelRackEntity> existing = jpaRepository.findByProjectId(rack.getProjectId());

        if (existing.isPresent()) {
            // UPDATE: modify the managed entity in place so JPA tracks changes correctly
            ChannelRackEntity entity = existing.get();
            entity.setBpm(rack.getBpm());

            // Clear existing channels (orphanRemoval deletes them from DB)
            entity.getChannels().clear();

            // Add the current channels from the domain model
            for (Channel ch : rack.getChannels()) {
                ChannelEntity ce = mapper.channelToEntity(ch);
                ce.setRack(entity);
                entity.getChannels().add(ce);
            }

            return mapper.toDomain(jpaRepository.save(entity));
        } else {
            // INSERT: first time save — create a new entity
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(rack)));
        }
    }

    @Override
    public Optional<ChannelRack> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).map(mapper::toDomain);
    }

    @Override
    public ChannelRack getByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new RuntimeException("No rack found for project: " + projectId));
    }
}