package org.eci.ZwingBackend.presence.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserColorTest {

    @Test
    void allHexReturnsPaletteInDeclaredOrder() {
        assertThat(UserColor.allHex()).containsExactly(
                "#E91E63",
                "#2196F3",
                "#4CAF50",
                "#FF9800",
                "#9C27B0",
                "#009688",
                "#F44336",
                "#FFC107");
    }
}