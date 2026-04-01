package org.eci.ZwingBackend.rack.application.service;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.port.in.ManageChannelCase;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ChannelService implements ManageChannelCase {
    @Override
    public Channel addChannel(UUID projectId, String name, UUID soundId, UUID requesterId) {
        return null;
    }

    @Override
    public void removeChannel(UUID projectId, UUID channelId, UUID requesterId) {

    }

    @Override
    public boolean toggleStep(UUID projectId, UUID channelId, int stepIndex, UUID requesterId) {
        return false;
    }

    @Override
    public Channel updateChannel(UUID projectId, UUID channelId, String name, UUID soundId, float volume, boolean active, UUID requesterId) {
        return null;
    }
}
