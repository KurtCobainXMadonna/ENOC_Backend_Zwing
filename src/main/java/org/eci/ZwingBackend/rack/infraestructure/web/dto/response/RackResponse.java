package org.eci.ZwingBackend.rack.infraestructure.web.dto.response;

import lombok.Data;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;

import java.util.List;
import java.util.UUID;

@Data
public class RackResponse {
    private UUID rackId;
    private UUID projectId;
    private int bpm;
    private List<ChannelResponse> channels;

    public static RackResponse from(ChannelRack rack) {
        RackResponse r = new RackResponse();
        r.rackId = rack.getRackId();
        r.projectId = rack.getProjectId();
        r.bpm = rack.getBpm();
        r.channels = rack.getChannels().stream().map(ChannelResponse::from).toList();
        return r;
    }

    @Data
    public static class ChannelResponse {
        private UUID channelId;
        private UUID rackId;
        private String name;
        private UUID soundId;
        private boolean active;
        private float volume;
        private boolean[] steps;
        private int position;

        public static ChannelResponse from(Channel channel) {
            ChannelResponse r = new ChannelResponse();
            r.channelId = channel.getChannelId();
            r.rackId = channel.getRackId();
            r.name = channel.getName();
            r.soundId = channel.getSoundId();
            r.active = channel.isActive();
            r.volume = channel.getVolume();
            r.steps = channel.getSteps();
            r.position = channel.getPosition();
            return r;
        }
    }
}
