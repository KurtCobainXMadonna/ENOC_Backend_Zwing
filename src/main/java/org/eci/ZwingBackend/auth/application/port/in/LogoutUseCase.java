package org.eci.ZwingBackend.auth.application.port.in;

public interface LogoutUseCase {
    void logout(String refreshToken);
}
