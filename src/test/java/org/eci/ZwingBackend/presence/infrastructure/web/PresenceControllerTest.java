package org.eci.ZwingBackend.presence.infrastructure.web;

import org.eci.ZwingBackend.presence.application.port.in.ManagePresenceCase;
import org.eci.ZwingBackend.presence.domain.model.Presence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceControllerTest {

    @Mock
    private ManagePresenceCase presenceCase;

    @InjectMocks
    private PresenceController controller;

    @Test
    void getRosterReturnsGeneralResponse() {
        UUID projectId = UUID.randomUUID();
        Presence presence = new Presence(projectId, "user", "user@example.com", "User", "#fff", Instant.now());
        when(presenceCase.getRoster(projectId)).thenReturn(List.of(presence));

        var response = controller.getRoster(projectId, "user");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("Success");
        assertThat(response.getBody().getMessage()).isEqualTo("Presence roster retrieved");
        assertThat(response.getBody().getData()).containsExactly(presence);
    }
}