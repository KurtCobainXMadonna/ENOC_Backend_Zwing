package org.eci.ZwingBackend.shared.config;

import org.eci.ZwingBackend.shared.websocket.JwtHandshakeInterceptor;
import org.eci.ZwingBackend.shared.websocket.StompPrincipalInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.messaging.simp.config.TaskExecutorRegistration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Mock
    private StompPrincipalInterceptor stompPrincipalInterceptor;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private SimpleBrokerRegistration simpleBrokerRegistration;

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;

    @Mock
    private SockJsServiceRegistration sockJsServiceRegistration;

    @Mock
    private ChannelRegistration channelRegistration;

    @Mock
    private TaskExecutorRegistration taskExecutorRegistration;  // ← este faltaba

    private WebSocketConfig config;

    @BeforeEach
    void setUp() {
        config = new WebSocketConfig(jwtHandshakeInterceptor, stompPrincipalInterceptor);
        ReflectionTestUtils.setField(config, "allowedOriginsCsv", "https://one.example, https://two.example ,");
    }

    @Test
    void configuresBrokerAndInboundChannel() {
        when(messageBrokerRegistry.enableSimpleBroker("/topic", "/queue")).thenReturn(simpleBrokerRegistration);
        when(simpleBrokerRegistration.setHeartbeatValue(new long[]{10000, 10000})).thenReturn(simpleBrokerRegistration);
        when(simpleBrokerRegistration.setTaskScheduler(any())).thenReturn(simpleBrokerRegistration);
        when(channelRegistration.taskExecutor()).thenReturn(taskExecutorRegistration);
        when(taskExecutorRegistration.corePoolSize(anyInt())).thenReturn(taskExecutorRegistration);
        when(taskExecutorRegistration.maxPoolSize(anyInt())).thenReturn(taskExecutorRegistration);
        when(taskExecutorRegistration.queueCapacity(anyInt())).thenReturn(taskExecutorRegistration);
        when(stompEndpointRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns("https://one.example", "https://two.example")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(jwtHandshakeInterceptor)).thenReturn(endpointRegistration);
        when(endpointRegistration.withSockJS()).thenReturn(sockJsServiceRegistration);

        config.configureMessageBroker(messageBrokerRegistry);
        config.registerStompEndpoints(stompEndpointRegistry);
        config.configureClientInboundChannel(channelRegistration);

        verify(messageBrokerRegistry).setApplicationDestinationPrefixes("/app");
        verify(messageBrokerRegistry).setUserDestinationPrefix("/user");
        verify(simpleBrokerRegistration).setHeartbeatValue(new long[]{10000, 10000});
        verify(simpleBrokerRegistration).setTaskScheduler(any());
        verify(channelRegistration).interceptors(stompPrincipalInterceptor);
        verify(channelRegistration).taskExecutor();
        verify(taskExecutorRegistration).corePoolSize(8);
        verify(taskExecutorRegistration).maxPoolSize(16);
        verify(taskExecutorRegistration).queueCapacity(200);
        verify(stompEndpointRegistry).addEndpoint("/ws");
        verify(endpointRegistration).setAllowedOriginPatterns("https://one.example", "https://two.example");
        verify(endpointRegistration).addInterceptors(jwtHandshakeInterceptor);
        verify(endpointRegistration).withSockJS();
    }
}