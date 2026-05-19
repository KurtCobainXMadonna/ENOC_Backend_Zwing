package org.eci.ZwingBackend.auth.infraestructure.events;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.auth.application.service.RefreshTokenService;
import org.eci.ZwingBackend.shared.events.UserDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthEventListener {
    private final RefreshTokenService refreshTokenService;

    @EventListener
    public void onUserDeleted(UserDeletedEvent event) {
        refreshTokenService.revokeAllForUser(event.getUserId());
    }
}
