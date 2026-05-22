package org.eci.ZwingBackend.auth.infraestructure.adapters.out;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenAdapterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void generateTokenProducesSignedJwtWithClaims() {
        JwtTokenAdapter adapter = new JwtTokenAdapter(SECRET, 900);
        User user = new User(UUID.randomUUID(), "Ada", "ada@example.com");

        String token = adapter.generateToken(user);

        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("ada@example.com");
        assertThat(String.valueOf(claims.get("userId"))).isEqualTo(user.getUserId().toString());
        assertThat(adapter.getExpirationSeconds()).isEqualTo(900);
    }
}