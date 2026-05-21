package org.eci.ZwingBackend.shared.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneralResponseTest {

    @Test
    void successBuildsExpectedResponse() {
        GeneralResponse<String> response = GeneralResponse.success("payload", "done");

        assertThat(response.getStatus()).isEqualTo("Success");
        assertThat(response.getMessage()).isEqualTo("done");
        assertThat(response.getData()).isEqualTo("payload");
    }

    @Test
    void errorBuildsExpectedResponse() {
        GeneralResponse<Void> response = GeneralResponse.error("boom");

        assertThat(response.getStatus()).isEqualTo("Error");
        assertThat(response.getMessage()).isEqualTo("boom");
        assertThat(response.getData()).isNull();
    }
}