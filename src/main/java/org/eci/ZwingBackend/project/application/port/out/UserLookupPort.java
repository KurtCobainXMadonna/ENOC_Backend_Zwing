package org.eci.ZwingBackend.project.application.port.out;

import org.eci.ZwingBackend.auth.domain.model.User;

import java.util.UUID;

public interface UserLookupPort {
    User getUserByEmail(String email);
    User getUserById(UUID userId);
}
