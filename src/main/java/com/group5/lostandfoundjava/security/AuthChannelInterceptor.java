package com.group5.lostandfoundjava.security;

import com.group5.lostandfoundjava.repository.ConversationRepository;
import com.group5.lostandfoundjava.service.TokenService;
import io.jsonwebtoken.Claims;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final String UUID_PATTERN = "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})";
    private static final Pattern SUBSCRIBE_DESTINATION = Pattern.compile("/topic/conversations/" + UUID_PATTERN);
    private static final Pattern SEND_DESTINATION = Pattern.compile("/app/conversations/" + UUID_PATTERN + "/send");

    private final JwtProvider jwtProvider;
    private final TokenService tokenService;
    private final ConversationRepository conversations;
    private final Map<String, SessionAuth> sessions = new ConcurrentHashMap<>();

    public AuthChannelInterceptor(JwtProvider jwtProvider, TokenService tokenService,
            ConversationRepository conversations) {
        this.jwtProvider = jwtProvider;
        this.tokenService = tokenService;
        this.conversations = conversations;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getSessionId() == null) {
            throw denied();
        }
        String sessionId = accessor.getSessionId();
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.DISCONNECT) {
            sessions.remove(sessionId);
            return message;
        }
        if (command == StompCommand.CONNECT) {
            String header = accessor.getFirstNativeHeader("Authorization");
            String token = header == null ? "" : header.replaceFirst("^Bearer ", "").trim();
            Claims claims = validClaims(token);
            if (claims == null) {
                throw denied();
            }
            UUID userId;
            try {
                userId = jwtProvider.userIdFrom(claims);
            } catch (IllegalArgumentException ex) {
                throw denied();
            }
            var principal = new UsernamePasswordAuthenticationToken(
                    userId.toString(), null, jwtProvider.roleFrom(claims).getAuthorities());
            if (sessions.putIfAbsent(sessionId, new SessionAuth(userId, token)) != null) {
                throw denied();
            }
            accessor.setUser(principal);
            return message;
        }

        SessionAuth auth = activeSession(sessionId);
        if (auth == null || accessor.getUser() == null
                || !auth.userId().toString().equals(accessor.getUser().getName())) {
            throw denied();
        }
        if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
            Pattern allowed = command == StompCommand.SUBSCRIBE ? SUBSCRIBE_DESTINATION : SEND_DESTINATION;
            if (!canAccess(auth, accessor.getDestination(), allowed)) {
                throw denied();
            }
        } else if (command != StompCommand.UNSUBSCRIBE
                && accessor.getMessageType() != SimpMessageType.HEARTBEAT) {
            throw denied();
        }
        return message;
    }

    // Check deliveries too: an idle subscriber must not keep receiving after logout or expiry.
    public ChannelInterceptor outboundInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
                if (accessor.getMessageType() != SimpMessageType.MESSAGE) {
                    return message;
                }
                SessionAuth auth = activeSession(accessor.getSessionId());
                return auth != null && canAccess(auth, accessor.getDestination(), SUBSCRIBE_DESTINATION)
                        ? message : null;
            }
        };
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        sessions.remove(event.getSessionId());
    }

    private SessionAuth activeSession(String sessionId) {
        SessionAuth auth = sessionId == null ? null : sessions.get(sessionId);
        if (auth != null && validClaims(auth.token()) == null) {
            sessions.remove(sessionId, auth);
            return null;
        }
        return auth;
    }

    private Claims validClaims(String token) {
        Claims claims = jwtProvider.parse(token);
        return claims != null && jwtProvider.isAccessToken(claims) && tokenService.isActive(token) ? claims : null;
    }

    private boolean canAccess(SessionAuth auth, String destination, Pattern allowed) {
        if (destination == null) {
            return false;
        }
        var matcher = allowed.matcher(destination);
        return matcher.matches()
                && conversations.existsForParticipant(UUID.fromString(matcher.group(1)), auth.userId());
    }

    private MessageDeliveryException denied() {
        return new MessageDeliveryException("WebSocket authentication or conversation access denied");
    }

    private record SessionAuth(UUID userId, String token) {}
}
