package com.group5.lostandfoundjava.security;

import io.jsonwebtoken.Claims;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * The WebSocket equivalent of {@link JwtAuthenticationFilter}.
 *
 * <p>A WebSocket has no HTTP headers after the handshake, so the token is checked once, on the STOMP
 * CONNECT frame. Unlike the HTTP filter this one <em>rejects</em> the connection when the token is
 * missing or invalid — there is no such thing as an anonymous chat session.
 *
 * <p>The authenticated user is attached to the session, which is what lets
 * {@link com.group5.lostandfoundjava.controller.ChatWebSocketController} receive a {@code Principal}.
 */
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public AuthChannelInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String header = accessor.getFirstNativeHeader("Authorization");
            Claims claims = null;
            if (header != null) {
                String token = header.startsWith(BEARER_PREFIX)
                        ? header.substring(BEARER_PREFIX.length()).trim()
                        : header.trim();
                claims = jwtProvider.parse(token);
            }
            if (claims == null || !jwtProvider.isAccessToken(claims)) {
                throw new MessageDeliveryException("Missing or invalid JWT in STOMP CONNECT");
            }

            accessor.setUser(
                    new UsernamePasswordAuthenticationToken(
                            jwtProvider.userIdFrom(claims).toString(),
                            null,
                            List.of(new SimpleGrantedAuthority(jwtProvider.roleFrom(claims).authority()))));
        }
        return message;
    }
}
