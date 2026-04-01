package org.eci.ZwingBackend.rack.domain.model;

import lombok.Data;

import java.util.UUID;

@Data
public class Channel {
    private UUID channelId;
    private UUID rackId;
    private String name;
    private UUID soundId;
    private boolean active;
    private float volume;
    private boolean[] steps;
    private int position;

    public Channel(UUID channelId, UUID rackId, String name, UUID soundId, int position) {
        this.channelId = channelId;
        this.rackId = rackId;
        this.name = name;
        this.soundId = soundId;
        this.active = true;
        this.volume = 1.0f;
        this.steps = new boolean[16];
        this.position = position;
    }

    public boolean toggleStep(int stepIndex) {
        if (stepIndex < 0 || stepIndex >= 16) {
            throw new IllegalArgumentException("Step index must be 0-15, got: " + stepIndex);
        }
        steps[stepIndex] = !steps[stepIndex];
        return steps[stepIndex];
    }
}
