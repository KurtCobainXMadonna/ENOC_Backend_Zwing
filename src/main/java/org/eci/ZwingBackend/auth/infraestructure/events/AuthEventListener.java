package org.eci.ZwingBackend.auth.infraestructure.events;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.auth.infraestructure.security.config.TokenBlacklistService;
import org.eci.ZwingBackend.shared.events.UserDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthEventListener {

    private final TokenBlacklistService tokenBlacklistService;

    @EventListener
    public void onUserDeleted(UserDeletedEvent event) {
        tokenBlacklistService.blacklistUser(event.getUserId().toString(), 86400);
    }
}
