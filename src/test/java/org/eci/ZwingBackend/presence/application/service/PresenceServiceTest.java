package org.eci.ZwingBackend.presence.application.service;

import org.eci.ZwingBackend.presence.application.port.out.PresenceStorePort;
import org.eci.ZwingBackend.presence.domain.model.Presence;
import org.eci.ZwingBackend.presence.domain.model.UserColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private PresenceStorePort store;

    private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        presenceService = new PresenceService(store);
    }

    @Test
    void userJoinedReturnsExistingPresence() {
        UUID projectId = UUID.randomUUID();
        Presence presence = new Presence(projectId, "user", "user@example.com", "User", "#123456", Instant.now());
        when(store.findPresence(projectId, "user")).thenReturn(Optional.of(presence));

        Presence result = presenceService.userJoined(projectId, "user", "user@example.com", "User");

        assertThat(result).isSameAs(presence);
        verify(store, never()).savePresence(presence);
    }

    @Test
    void userJoinedUsesProvidedDisplayNameAndClaimedColor() {
        UUID projectId = UUID.randomUUID();
        when(store.findPresence(projectId, "user")).thenReturn(Optional.empty());
        when(store.claimColor(projectId, UserColor.allHex())).thenReturn(Optional.of("#abcdef"));
        when(store.countInProject(projectId)).thenReturn(1L);

        Presence result = presenceService.userJoined(projectId, "user", "user@example.com", "Display Name");

        assertThat(result.getDisplayName()).isEqualTo("Display Name");
        assertThat(result.getColor()).isEqualTo("#abcdef");
        verify(store).savePresence(result);
    }

    @Test
    void userJoinedFallsBackToEmailAndPaletteColor() {
        UUID projectId = UUID.randomUUID();
        String userId = "user-id";
        when(store.findPresence(projectId, userId)).thenReturn(Optional.empty());
        when(store.claimColor(projectId, UserColor.allHex())).thenReturn(Optional.empty());
        when(store.countInProject(projectId)).thenReturn(0L);

        Presence result = presenceService.userJoined(projectId, userId, "user@example.com", null);

        int index = Math.floorMod(userId.hashCode(), UserColor.allHex().size());
        assertThat(result.getDisplayName()).isEqualTo("user@example.com");
        assertThat(result.getColor()).isEqualTo(UserColor.allHex().get(index));
    }

    @Test
    void userLeftReturnsTrueWhenLastUserLeaves() {
        UUID projectId = UUID.randomUUID();
        Presence presence = new Presence(projectId, "user", "user@example.com", "User", UserColor.BLUE.hex(), Instant.now());
        when(store.findPresence(projectId, "user")).thenReturn(Optional.of(presence));
        when(store.countInProject(projectId)).thenReturn(0L);

        boolean empty = presenceService.userLeft(projectId, "user");

        assertThat(empty).isTrue();
        verify(store).removePresence(projectId, "user");
        verify(store).releaseColor(projectId, UserColor.BLUE.hex());
    }

    @Test
    void userLeftReturnsStoreCountWhenEntryMissing() {
        UUID projectId = UUID.randomUUID();
        when(store.findPresence(projectId, "user")).thenReturn(Optional.empty());
        when(store.countInProject(projectId)).thenReturn(2L);

        boolean empty = presenceService.userLeft(projectId, "user");

        assertThat(empty).isFalse();
    }

    @Test
    void rosterAndCountDelegateToStore() {
        UUID projectId = UUID.randomUUID();
        Presence presence = new Presence(projectId, "user", "user@example.com", "User", "#123456", Instant.now());
        when(store.findAllInProject(projectId)).thenReturn(List.of(presence));
        when(store.countInProject(projectId)).thenReturn(1L);

        assertThat(presenceService.getRoster(projectId)).containsExactly(presence);
        assertThat(presenceService.getUserCount(projectId)).isEqualTo(1L);
    }
}