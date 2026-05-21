package org.eci.ZwingBackend.auth.infraestructure.events;

import org.eci.ZwingBackend.auth.application.service.RefreshTokenService;
import org.eci.ZwingBackend.shared.events.UserDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthEventListenerTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    void revokesAllRefreshTokensWhenUserIsDeleted() {
        AuthEventListener listener = new AuthEventListener(refreshTokenService);
        UUID userId = UUID.randomUUID();

        listener.onUserDeleted(new UserDeletedEvent(userId));

        verify(refreshTokenService).revokeAllForUser(userId);
    }
}