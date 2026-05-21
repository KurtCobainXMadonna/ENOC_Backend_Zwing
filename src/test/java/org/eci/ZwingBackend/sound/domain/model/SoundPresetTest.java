package org.eci.ZwingBackend.sound.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SoundPresetTest {

    @Test
    void isGlobalDependsOnProjectId() {
        SoundPreset globalPreset = new SoundPreset(UUID.randomUUID(), "Kick", SoundCategory.KICK, "url", "desc", null, null);
        SoundPreset projectPreset = new SoundPreset(UUID.randomUUID(), "Kick", SoundCategory.KICK, "url", "desc", UUID.randomUUID(), UUID.randomUUID());

        assertThat(globalPreset.isGlobal()).isTrue();
        assertThat(projectPreset.isGlobal()).isFalse();
    }
}