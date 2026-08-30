package com.group5.lostandfoundjava.config;

import com.group5.lostandfoundjava.security.AuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time chat over STOMP on top of WebSocket.
 *
 * <p>How the pieces fit together:
 *
 * <ul>
 *   <li>the client connects to {@code /ws}, sending its access token in the CONNECT frame
 *   <li>it subscribes to {@code /topic/conversations/{id}} to receive messages
 *   <li>it publishes to {@code /app/conversations/{id}/send} to post one
 * </ul>
 *
 * <p>The broker is the built-in in-memory one, which is enough for a single instance. Running
 * several instances would need a real broker (RabbitMQ, ActiveMQ) so subscribers on one instance see
 * messages published on another.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptor authChannelInterceptor;

    public WebSocketConfig(AuthChannelInterceptor authChannelInterceptor) {
        this.authChannelInterceptor = authChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    /** Hooks JWT checking into every inbound frame. See {@link AuthChannelInterceptor}. */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
