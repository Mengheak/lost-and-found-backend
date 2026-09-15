package com.group5.lostandfoundjava.security;

import com.group5.lostandfoundjava.service.TokenService;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

// The WebSocket equivalent of JwtAuthenticationFilter
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final TokenService tokenService;

    public AuthChannelInterceptor(JwtProvider jwtProvider, TokenService tokenService) {
        this.jwtProvider = jwtProvider;
        this.tokenService = tokenService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String header = accessor.getFirstNativeHeader("Authorization");
            Claims claims = null;
            String token = null;
            if (header != null) {
                token = header.startsWith(BEARER_PREFIX)
                        ? header.substring(BEARER_PREFIX.length()).trim()
                        : header.trim();
                claims = jwtProvider.parse(token);
            }
            // Revoked tokens are refused here too, so logging out also shuts the socket out.
            if (claims == null || !jwtProvider.isAccessToken(claims) || !tokenService.isActive(token)) {
                throw new MessageDeliveryException("Missing or invalid JWT in STOMP CONNECT");
            }

            accessor.setUser(
                    new UsernamePasswordAuthenticationToken(
                            jwtProvider.userIdFrom(claims).toString(),
                            null,
                            jwtProvider.roleFrom(claims).getAuthorities()));
        }
        return message;
    }
}
