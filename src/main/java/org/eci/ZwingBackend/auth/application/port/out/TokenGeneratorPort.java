package org.eci.ZwingBackend.auth.application.port.out;

import org.eci.ZwingBackend.auth.domain.model.User;

public interface TokenGeneratorPort {
    String generateToken(User user);
}
