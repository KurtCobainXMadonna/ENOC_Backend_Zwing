package org.eci.ZwingBackend.auth.application.port.out;

import org.eci.ZwingBackend.auth.domain.model.GoogleUserData;

public interface GoogleAuthPort {
    GoogleUserData verifyToken(String googleToken);
}
