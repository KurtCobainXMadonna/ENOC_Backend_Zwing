package org.eci.ZwingBackend.rack.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelRackTest {

    @Test
    void removeChannelReordersRemainingChannels() {
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), UUID.randomUUID());
        Channel first = new Channel(UUID.randomUUID(), rack.getRackId(), "One", UUID.randomUUID(), 0);
        Channel second = new Channel(UUID.randomUUID(), rack.getRackId(), "Two", UUID.randomUUID(), 1);
        rack.addChannel(first);
        rack.addChannel(second);

        rack.removeChannel(first.getChannelId());

        assertThat(rack.getChannels()).containsExactly(second);
        assertThat(second.getPosition()).isZero();
    }

    @Test
    void getChannelThrowsWhenMissing() {
        ChannelRack rack = new ChannelRack(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> rack.getChannel(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Channel not found");
    }
}