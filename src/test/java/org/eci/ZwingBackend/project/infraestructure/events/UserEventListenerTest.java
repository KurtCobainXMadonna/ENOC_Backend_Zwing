package org.eci.ZwingBackend.project.infraestructure.events;

import org.eci.ZwingBackend.project.application.port.in.HandleUserDeletionCase;
import org.eci.ZwingBackend.shared.events.UserDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock
    private HandleUserDeletionCase handleUserDeletionCase;

    @Test
    void removesUserFromAllProjectsWhenDeleted() {
        UserEventListener listener = new UserEventListener(handleUserDeletionCase);
        UUID userId = UUID.randomUUID();

        listener.onUserDeleted(new UserDeletedEvent(userId));

        verify(handleUserDeletionCase).removeUserFromAllProjects(userId);
    }
}