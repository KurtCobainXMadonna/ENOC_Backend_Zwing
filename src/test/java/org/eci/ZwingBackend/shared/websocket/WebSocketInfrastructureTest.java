package org.eci.ZwingBackend.shared.websocket;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.eci.ZwingBackend.presence.application.port.in.ManagePresenceCase;
import org.eci.ZwingBackend.presence.domain.model.Presence;
import org.eci.ZwingBackend.project.application.port.in.ManagingProjectsCase;
import org.eci.ZwingBackend.project.domain.model.Project;
import org.eci.ZwingBackend.rack.application.service.RackSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketInfrastructureTest {

    @Mock
    private RackSessionService rackSessionService;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, String> valueOperations;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ManagePresenceCase presenceCase;

    @Mock
    private ManagingProjectsCase managingProjectsCase;

    @Mock
    private WebSocketEventListener presenceTracker;

    private WebSocketEventListener eventListener;
    private ProjectWebSocketController projectWebSocketController;
    private JwtHandshakeInterceptor handshakeInterceptor;
    private StompPrincipalInterceptor principalInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        eventListener = new WebSocketEventListener(rackSessionService, redisTemplate, messagingTemplate, presenceCase);
        projectWebSocketController = new ProjectWebSocketController(messagingTemplate, managingProjectsCase, presenceTracker);
        handshakeInterceptor = new JwtHandshakeInterceptor();
        principalInterceptor = new StompPrincipalInterceptor();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        setField(handshakeInterceptor, "jwtSecret", "0123456789abcdef0123456789abcdef");
    }

    @Test
    void registerAndUnregisterUserBroadcastPresence() {
        UUID projectId = UUID.randomUUID();
        Presence presence = new Presence(projectId, "user", "user@example.com", "User", "#123456", java.time.Instant.now());
        when(presenceCase.userJoined(projectId, "user", "user@example.com", "User")).thenReturn(presence);
        when(presenceCase.getRoster(projectId)).thenReturn(List.of(presence));
        when(presenceCase.userLeft(projectId, "user")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Presence result = eventListener.registerUserInProject("session-1", "user", projectId.toString(), "user@example.com", "User");
        eventListener.unregisterUserFromProject("session-1", "user", projectId.toString());

        assertThat(result).isSameAs(presence);
        verify(valueOperations).set("ws_session:session-1", projectId + "::user");
        verify(redisTemplate).delete("ws_session:session-1");
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/project/" + projectId + "/presence"), any(org.eci.ZwingBackend.presence.infrastructure.websocket.dto.PresenceEvent.class));
        verify(rackSessionService).flushToDatabase(projectId);
    }

    @Test
    void handleSessionDisconnectReleasesLocksAndBroadcasts() {
        UUID projectId = UUID.randomUUID();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", "user");
        attrs.put("email", "user@example.com");
        String sessionId = "session-2";
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeader("simpSessionAttributes", attrs)
                .build();
        when(redisTemplate.opsForValue().get("ws_session:" + sessionId)).thenReturn(projectId + "::user");
        when(presenceCase.userLeft(projectId, "user")).thenReturn(false);
        when(presenceCase.getRoster(projectId)).thenReturn(List.of());

        eventListener.handleSessionDisconnect(new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL));

        verify(rackSessionService).releasePlaybackLockIfHolder(projectId, "user");
        verify(rackSessionService).releaseAllChannelLocks(projectId, "user");
        verify(messagingTemplate).convertAndSend(eq("/topic/rack/" + projectId), any(java.util.Map.class));
    }

    @Test
    void jwtHandshakeInterceptorExtractsTokenAndClaims() throws Exception {
        String secret = "0123456789abcdef0123456789abcdef";
        setField(handshakeInterceptor, "jwtSecret", secret);
        String token = Jwts.builder()
                .subject("user@example.com")
                .claim("userId", "123e4567-e89b-12d3-a456-426614174000")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setCookies(new jakarta.servlet.http.Cookie("jwt_token", token));
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = handshakeInterceptor.beforeHandshake(new ServletServerHttpRequest(servletRequest), new ServletServerHttpResponse(new MockHttpServletResponse()), mockWebSocketHandler(), attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes)
            .containsEntry("userId", "123e4567-e89b-12d3-a456-426614174000")
            .containsEntry("email", "user@example.com")
            .containsKey("token");
    }

    @Test
    void stompPrincipalInterceptorSetsPrincipalOnConnect() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(Map.of("userId", "user-123"));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = principalInterceptor.preSend(message, mockMessageChannel());

        assertThat(result).isSameAs(message);
        StompHeaderAccessor updated = StompHeaderAccessor.wrap(result);
        Principal principal = updated.getUser();
        assertThat(principal).isNotNull();
        assertThat(principal.getName()).isEqualTo("user-123");
    }

    @Test
    void projectWebSocketJoinAndLeaveDelegateToPresenceTracker() {
        UUID projectId = UUID.randomUUID();
        String sessionId = "session-3";
        UUID userId = UUID.randomUUID();
        Project project = new Project("Project");
        Presence presence = new Presence(projectId, "user", "user@example.com", "User", "#fff", java.time.Instant.now());
        when(managingProjectsCase.getProjectById(projectId, userId)).thenReturn(project);
        when(presenceTracker.registerUserInProject(sessionId, userId.toString(), projectId.toString(), "user@example.com", null)).thenReturn(presence);

        SimpHeaderAccessorBuilder builder = new SimpHeaderAccessorBuilder(sessionId, userId.toString(), "user@example.com");
        projectWebSocketController.joinProject(projectId.toString(), builder.accessor);
        projectWebSocketController.leaveProject(projectId.toString(), builder.accessor);

        verify(presenceTracker).registerUserInProject(sessionId, userId.toString(), projectId.toString(), "user@example.com", null);
        verify(presenceTracker).unregisterUserFromProject(sessionId, userId.toString(), projectId.toString());
    }

    private WebSocketHandler mockWebSocketHandler() {
        return mock(WebSocketHandler.class);
    }

    private MessageChannel mockMessageChannel() {
        return mock(MessageChannel.class);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class SimpHeaderAccessorBuilder {
        final SimpMessageHeaderAccessor accessor;

        SimpHeaderAccessorBuilder(String sessionId, String userId, String email) {
            accessor = SimpMessageHeaderAccessor.create();
            accessor.setSessionId(sessionId);
            accessor.setSessionAttributes(new HashMap<>(Map.of("userId", userId, "email", email)));
        }
    }
}