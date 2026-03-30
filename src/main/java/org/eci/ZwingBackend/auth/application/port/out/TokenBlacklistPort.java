package org.eci.ZwingBackend.auth.application.port.out;

public interface TokenBlacklistPort {
    void blacklistToken(String token, long durationSeconds);
    boolean isTokenBlacklisted(String token);
}
