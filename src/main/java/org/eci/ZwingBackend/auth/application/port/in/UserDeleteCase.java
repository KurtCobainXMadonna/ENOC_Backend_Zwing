package org.eci.ZwingBackend.auth.application.port.in;

import java.util.UUID;

public interface UserDeleteCase {
    void deleteUser(UUID userID);
}
