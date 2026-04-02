package org.eci.ZwingBackend.rack.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ChannelRack {
    private UUID rackId;
    private UUID projectId;
    private int bpm;
    private List<Channel> channels;

    public ChannelRack(UUID rackId, UUID projectId) {
        this.rackId = rackId;
        this.projectId = projectId;
        this.bpm = 120;
        this.channels = new ArrayList<>();
    }

    public void addChannel(Channel channel) {
        channels.add(channel);
    }

    public void removeChannel(UUID channelId) {
        channels.removeIf(c -> c.getChannelId().equals(channelId));
        reorderPositions();
    }

    public Channel getChannel(UUID channelId) {
        return channels.stream()
                .filter(c -> c.getChannelId().equals(channelId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Channel not found: " + channelId));
    }

    private void reorderPositions() {
        for (int i = 0; i < channels.size(); i++) {
            channels.get(i).setPosition(i);
        }
    }
}
