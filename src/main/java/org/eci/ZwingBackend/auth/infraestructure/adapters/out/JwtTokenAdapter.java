package org.eci.ZwingBackend.auth.infraestructure.adapters.out;

import org.eci.ZwingBackend.auth.application.port.out.TokenGeneratorPort;
import org.eci.ZwingBackend.auth.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenAdapter implements TokenGeneratorPort {
    private final SecretKey key;
    private final long expirationMillis;

    public JwtTokenAdapter(@Value("${jwt.secret}") String secret, @Value("${auth.access-token.ttl-seconds:900}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = ttlSeconds * 1000L;
    }

    @Override
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getUserId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000L;
    }
}
