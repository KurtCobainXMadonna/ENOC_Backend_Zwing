package org.eci.ZwingBackend.rack.infraestructure.persistence.postgre;

import org.eci.ZwingBackend.rack.infraestructure.persistence.entity.ChannelRackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChannelRackRepository extends JpaRepository<ChannelRackEntity, UUID> {
    Optional<ChannelRackEntity> findByProjectId(UUID projectId);
}
