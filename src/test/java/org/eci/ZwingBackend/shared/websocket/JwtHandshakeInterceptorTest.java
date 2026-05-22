package org.eci.ZwingBackend.shared.websocket;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtHandshakeInterceptorTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private JwtHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtHandshakeInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtSecret", SECRET);
    }

    @Test
    void rejectsNonServletRequests() {
        assertThat(interceptor.beforeHandshake(mock(ServerHttpRequest.class), mock(ServerHttpResponse.class), mock(WebSocketHandler.class), new HashMap<>()))
                .isFalse();
    }

    @Test
    void rejectsRequestsWithoutJwtCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);

        assertThat(interceptor.beforeHandshake(serverRequest, mock(ServerHttpResponse.class), mock(WebSocketHandler.class), new HashMap<>()))
                .isFalse();
    }

    @Test
    void acceptsValidTokenAndStoresClaims() {
        String token = signedToken();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("jwt_token", token));
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(serverRequest, mock(ServerHttpResponse.class), mock(WebSocketHandler.class), attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes).containsEntry("userId", "11111111-1111-1111-1111-111111111111");
        assertThat(attributes).containsEntry("email", "ada@example.com");
        assertThat(attributes).containsEntry("token", token);
    }

    @Test
    void rejectsInvalidToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("jwt_token", "not-a-valid-token"));
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);

        assertThat(interceptor.beforeHandshake(serverRequest, mock(ServerHttpResponse.class), mock(WebSocketHandler.class), new HashMap<>()))
                .isFalse();
    }

    private static String signedToken() {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("ada@example.com")
                .claim("userId", UUID.fromString("11111111-1111-1111-1111-111111111111").toString())
                .signWith(key)
                .compact();
    }
}