package com.group5.lostandfoundjava.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.group5.lostandfoundjava.config.JwtProperties;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.ConversationRepository;
import com.group5.lostandfoundjava.service.TokenService;
import java.security.Principal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

class AuthChannelInterceptorTest {
    private final JwtProvider jwt = spy(new JwtProvider(new JwtProperties(
            "test-secret-0123456789abcdef0123456789abcdef", Duration.ofMinutes(15), Duration.ofDays(7))));
    private final TokenService tokens = mock(TokenService.class);
    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final AuthChannelInterceptor interceptor = new AuthChannelInterceptor(jwt, tokens, conversations);
    private final UUID user = UUID.randomUUID();
    private final UUID conversation = UUID.randomUUID();
    private final String token = jwt.generateAccessToken(user, Role.USER);
    private Principal principal;

    private Message<byte[]> frame(StompCommand command, String destination, String authorization) {
        var accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session");
        accessor.setUser(principal);
        if (destination != null) accessor.setDestination(destination);
        if (authorization != null) accessor.setNativeHeader("Authorization", authorization);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private void connect() {
        when(tokens.isActive(token)).thenReturn(true);
        var message = frame(StompCommand.CONNECT, null, "Bearer " + token);
        interceptor.preSend(message, null);
        principal = StompHeaderAccessor.wrap(message).getUser();
        assertEquals(user.toString(), principal.getName());
    }

    private Message<byte[]> delivery() {
        // The simple broker sends SIMP messages, not necessarily STOMP accessors.
        var accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId("session");
        accessor.setDestination("/topic/conversations/" + conversation);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void participantsCanSubscribeSendAndReceive() {
        connect();
        when(conversations.existsForParticipant(conversation, user)).thenReturn(true);
        var subscribe = frame(StompCommand.SUBSCRIBE, "/topic/conversations/" + conversation, null);
        var send = frame(StompCommand.SEND, "/app/conversations/" + conversation + "/send", null);
        assertSame(subscribe, interceptor.preSend(subscribe, null));
        assertSame(send, interceptor.preSend(send, null));
        var delivery = delivery();
        assertSame(delivery, interceptor.outboundInterceptor().preSend(delivery, null));
    }

    @Test
    void outsidersCannotSubscribeSendOrReceive() {
        connect();
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/conversations/" + conversation, null), null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                frame(StompCommand.SEND, "/app/conversations/" + conversation + "/send", null), null));
        assertNull(interceptor.outboundInterceptor().preSend(delivery(), null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/conversations/*", "/topic/conversations/**", "/queue/private",
            "/app/conversations/123/send", "/topic/conversations/not-a-uuid"})
    void rejectsUnexpectedSubscriptionDestinations(String destination) {
        connect();
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, destination, null), null));
    }

    @Test
    void rejectsDirectBrokerSendEvenForParticipant() {
        connect();
        when(conversations.existsForParticipant(conversation, user)).thenReturn(true);
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                frame(StompCommand.SEND, "/topic/conversations/" + conversation, null), null));
    }

    @Test
    void rejectsMissingInvalidRefreshAndRevokedCredentials() {
        for (String credential : new String[] {"", "invalid", jwt.generateRefreshToken(user), token}) {
            assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                    frame(StompCommand.CONNECT, null, credential), null));
        }
    }

    @Test
    void rejectsFramesWithoutAuthenticatedSession() {
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/conversations/" + conversation, null), null));
        assertNull(interceptor.outboundInterceptor().preSend(delivery(), null));
    }

    @Test
    void revocationStopsExistingSubscriberAndFurtherSends() {
        connect();
        when(conversations.existsForParticipant(conversation, user)).thenReturn(true);
        when(tokens.isActive(token)).thenReturn(false);
        assertNull(interceptor.outboundInterceptor().preSend(delivery(), null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                frame(StompCommand.SEND, "/app/conversations/" + conversation + "/send", null), null));
    }

    @Test
    void expiryStopsExistingSubscriberAndFurtherSends() {
        connect();
        when(conversations.existsForParticipant(conversation, user)).thenReturn(true);
        doReturn(null).when(jwt).parse(token);
        assertNull(interceptor.outboundInterceptor().preSend(delivery(), null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                frame(StompCommand.SEND, "/app/conversations/" + conversation + "/send", null), null));
    }

    @Test
    void transportDisconnectRemovesSessionIdempotently() {
        connect();
        var event = new SessionDisconnectEvent(this, frame(StompCommand.DISCONNECT, null, null),
                "session", CloseStatus.NORMAL);
        interceptor.onDisconnect(event);
        interceptor.onDisconnect(event);
        assertNull(interceptor.outboundInterceptor().preSend(delivery(), null));
    }
}
