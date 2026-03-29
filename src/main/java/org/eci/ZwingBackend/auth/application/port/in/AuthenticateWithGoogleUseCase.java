package org.eci.ZwingBackend.auth.application.port.in;

import org.eci.ZwingBackend.auth.infraestructure.web.dto.response.AuthResponse;

public interface AuthenticateWithGoogleUseCase {
    AuthResponse authenticate(String idToken);
}