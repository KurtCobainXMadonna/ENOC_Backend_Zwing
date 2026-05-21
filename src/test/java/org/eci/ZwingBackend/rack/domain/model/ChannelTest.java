package org.eci.ZwingBackend.rack.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelTest {

    @Test
    void toggleStepFlipsValue() {
        Channel channel = new Channel(UUID.randomUUID(), UUID.randomUUID(), "Kick", UUID.randomUUID(), 0);

        assertThat(channel.toggleStep(2)).isTrue();
        assertThat(channel.toggleStep(2)).isFalse();
    }

    @Test
    void toggleStepRejectsInvalidIndex() {
        Channel channel = new Channel(UUID.randomUUID(), UUID.randomUUID(), "Kick", UUID.randomUUID(), 0);

        assertThatThrownBy(() -> channel.toggleStep(16))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Step index must be 0-15");
    }
}