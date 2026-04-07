package org.eci.ZwingBackend.rack.infraestructure.persistence.repository.mapper;

import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.rack.infraestructure.persistence.entity.ChannelEntity;
import org.eci.ZwingBackend.rack.infraestructure.persistence.entity.ChannelRackEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class RackMapper {
    public ChannelRack toDomain(ChannelRackEntity entity) {
        ChannelRack rack = new ChannelRack(entity.getRackId(), entity.getProjectId());
        rack.setBpm(entity.getBpm());
        entity.getChannels().stream()
                .map(this::channelToDomain)
                .forEach(rack::addChannel);
        return rack;
    }
    public ChannelRackEntity toEntity(ChannelRack domain) {
        ChannelRackEntity entity = new ChannelRackEntity();
        entity.setRackId(domain.getRackId());
        entity.setProjectId(domain.getProjectId());
        entity.setBpm(domain.getBpm());

        var channelEntities = domain.getChannels().stream()
                .map(c -> {
                    ChannelEntity ce = channelToEntity(c);
                    ce.setRack(entity);
                    return ce;
                })
                .collect(Collectors.toList());

        entity.setChannels(channelEntities);
        return entity;
    }

    public Channel channelToDomain(ChannelEntity entity) {
        Channel channel = new Channel(
                entity.getChannelId(),
                entity.getRack().getRackId(),
                entity.getName(),
                entity.getSoundId(),
                entity.getPosition()
        );
        channel.setActive(entity.isActive());
        channel.setVolume(entity.getVolume());
        channel.setSteps(stepsFromString(entity.getSteps()));
        return channel;
    }
    public ChannelEntity channelToEntity(Channel domain) {
        ChannelEntity entity = new ChannelEntity();
        entity.setChannelId(domain.getChannelId());
        entity.setName(domain.getName());
        entity.setSoundId(domain.getSoundId());
        entity.setActive(domain.isActive());
        entity.setVolume(domain.getVolume());
        entity.setSteps(stepsToString(domain.getSteps()));
        entity.setPosition(domain.getPosition());
        return entity;
    }

    private String stepsToString(boolean[] steps) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.length; i++) {
            sb.append(steps[i]);
            if (i < steps.length - 1) sb.append(",");
        }
        return sb.toString();
    }
    private boolean[] stepsFromString(String steps) {
        String[] parts = steps.split(",");
        boolean[] result = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Boolean.parseBoolean(parts[i].trim());
        }
        return result;
    }
}
