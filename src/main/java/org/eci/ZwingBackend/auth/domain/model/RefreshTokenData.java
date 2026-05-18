package org.eci.ZwingBackend.auth.domain.model;

import java.util.UUID;

public record RefreshTokenData(UUID userId, String email) {}
