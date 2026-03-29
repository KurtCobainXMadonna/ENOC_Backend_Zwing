package org.eci.ZwingBackend.project.infraestructure.events;

import org.eci.ZwingBackend.auth.infraestructure.security.config.TokenBlacklistService;
import org.eci.ZwingBackend.project.application.port.in.HandleUserDeletionCase;
import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.shared.events.UserDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
@AllArgsConstructor
public class UserEventListener {
    private final HandleUserDeletionCase handleUserDeletionCase;
    private final TokenBlacklistService tokenBlacklistService;

    @EventListener
    public void onUserDeleted(UserDeletedEvent event) {
        handleUserDeletionCase.removeUserFromAllProjects(event.getUserId());
        tokenBlacklistService.blacklistUser(event.getUserId().toString(), 86400);
    }
}