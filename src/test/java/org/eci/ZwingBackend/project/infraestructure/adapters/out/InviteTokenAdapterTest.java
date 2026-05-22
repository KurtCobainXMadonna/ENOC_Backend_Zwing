package org.eci.ZwingBackend.project.infraestructure.adapters.out;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InviteTokenAdapterTest {

    @Test
    void generateTokenCreatesEightCharacterAlphanumericCode() {
        InviteTokenAdapter adapter = new InviteTokenAdapter();

        String token = adapter.generateToken();

        assertThat(token).hasSize(8);
        assertThat(token).matches("[A-Z0-9]{8}");
    }
}