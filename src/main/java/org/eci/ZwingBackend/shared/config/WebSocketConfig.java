package org.eci.ZwingBackend.shared.config;

import org.eci.ZwingBackend.shared.websocket.JwtHandshakeInterceptor;
import org.eci.ZwingBackend.shared.websocket.StompPrincipalInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompPrincipalInterceptor stompPrincipalInterceptor;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsCsv;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor,
                           StompPrincipalInterceptor stompPrincipalInterceptor) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.stompPrincipalInterceptor = stompPrincipalInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
                // Server sends heartbeats every 10s, expects client heartbeats every 10s.
                // Keeps connections alive through proxies/load balancers and detects dead clients quickly.
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(heartBeatScheduler());
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Sets the Principal on STOMP CONNECT so convertAndSendToUser works.
        registration.interceptors(stompPrincipalInterceptor);
        // Pool sizing for inbound message processing — handles many simultaneous edits.
        registration.taskExecutor()
                .corePoolSize(8)
                .maxPoolSize(16)
                .queueCapacity(200);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Pool sizing for outbound broadcasts — handles many simultaneous subscribers.
        registration.taskExecutor()
                .corePoolSize(8)
                .maxPoolSize(16)
                .queueCapacity(200);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        // Larger limits than default to handle bursts of edits / many subscribers per topic.
        registry.setMessageSizeLimit(128 * 1024);      // 128 KB per message (default 64 KB)
        registry.setSendBufferSizeLimit(1024 * 1024);  // 1 MB outbound buffer (default 512 KB)
        registry.setSendTimeLimit(20_000);             // 20 seconds (default 10s)
    }

    @Bean
    public ThreadPoolTaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}