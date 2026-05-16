package org.eci.ZwingBackend.auth.application.port.in;

import org.eci.ZwingBackend.auth.domain.model.TokenPair;

public interface RefreshSessionUseCase {
    TokenPair refresh(String rawRefreshToken);
}
