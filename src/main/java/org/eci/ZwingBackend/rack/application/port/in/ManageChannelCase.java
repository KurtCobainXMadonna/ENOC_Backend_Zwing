package org.eci.ZwingBackend.rack.application.port.in;

import org.eci.ZwingBackend.rack.domain.model.Channel;
import java.util.UUID;

public interface ManageChannelCase {
    Channel addChannel(UUID projectId, String name, UUID soundId, UUID requesterId);
    void removeChannel(UUID projectId, UUID channelId, UUID requesterId);

    boolean toggleStep(UUID projectId, UUID channelId, int stepIndex, UUID requesterId);

    Channel updateChannel(UUID projectId, UUID channelId, String name, UUID soundId, float volume, boolean active, UUID requesterId);
}
