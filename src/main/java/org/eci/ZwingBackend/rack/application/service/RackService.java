package org.eci.ZwingBackend.rack.application.service;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.port.in.ManageRackCase;
import org.eci.ZwingBackend.rack.application.port.out.RackRepositoryPort;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class RackService implements ManageRackCase {
    private final RackRepositoryPort rackRepository;

    @Override
    @Transactional
    public ChannelRack createRackForProject(UUID projectId) {
        return rackRepository. findByProjectId(projectId).orElseGet(() -> {
            ChannelRack rack = new ChannelRack(UUID.randomUUID(), projectId);
            return rackRepository.save(rack);
        });
    }

    @Override
    public ChannelRack getRackByProject(UUID projectId) {
        return rackRepository.getByProjectId(projectId);
    }
}
