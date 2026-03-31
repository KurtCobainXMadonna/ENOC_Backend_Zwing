package org.eci.ZwingBackend.shared.config;

import org.eci.ZwingBackend.shared.websocket.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // The server will broadcast to destinations prefixed with /topic (pub/sub, one-to-many)
        // and /queue (point-to-point, one user). You'll use /topic for project rooms.
        config.enableSimpleBroker("/topic", "/queue");

        // Messages the CLIENT sends TO the server must be prefixed with /app
        // e.g. client sends to /app/project/join → server @MessageMapping("/project/join") handles it
        config.setApplicationDestinationPrefixes("/app");

        // When the server wants to send to a specific user (not a topic),
        // it uses /user/{userId}/queue/... — Spring routes this automatically
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:5173")
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
    }
}