package org.eci.ZwingBackend.rack.application.port.in;

import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import java.util.UUID;

public interface ManageRackCase {
    ChannelRack createRackForProject(UUID projectId);
    ChannelRack getRackByProject(UUID projectId);
}
