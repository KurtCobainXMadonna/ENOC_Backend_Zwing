package org.eci.ZwingBackend.project.application.port.out;

import java.util.UUID;

public interface UserLookupPort {
    UUID getUserIdByEmail(String email);
}
