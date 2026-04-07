package org.eci.ZwingBackend.rack.application.port.out;

import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import java.util.Optional;
import java.util.UUID;

public interface RackRepositoryPort {
    ChannelRack save(ChannelRack rack);
    Optional<ChannelRack> findByProjectId(UUID projectId);
    ChannelRack getByProjectId(UUID projectId);
}
