package org.eci.ZwingBackend.rack.infraestructure.persistence.repository;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.port.out.RackRepositoryPort;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.rack.infraestructure.persistence.postgre.ChannelRackRepository;
import org.eci.ZwingBackend.rack.infraestructure.persistence.repository.mapper.RackMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class RackRepositoryAdapter implements RackRepositoryPort {
    private final ChannelRackRepository jpaRepository;
    private final RackMapper mapper;

    @Override
    public ChannelRack save(ChannelRack rack) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(rack)));
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
