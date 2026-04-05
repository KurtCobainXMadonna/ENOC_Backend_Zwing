package org.eci.ZwingBackend.shared.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

/**
 * Intercepts STOMP CONNECT frames and sets the Principal from the session attributes.
 *
 * JwtHandshakeInterceptor stores userId in session attributes during the HTTP handshake,
 * but Spring's convertAndSendToUser requires a Principal on the STOMP session.
 * This interceptor bridges that gap.
 */
@Component
public class StompPrincipalInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                String userId = (String) sessionAttributes.get("userId");
                if (userId != null) {
                    accessor.setUser(new StompPrincipal(userId));
                }
            }
        }

        return message;
    }

    /**
     * Simple Principal implementation backed by the userId string.
     */
    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}