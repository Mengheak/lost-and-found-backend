package com.group5.lostandfoundjava.dto.chat;

import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/conversations/{id}/messages}, and also the payload sent over WebSocket.
 *
 * <p>Both fields are optional on their own, but at least one of them must be present — a rule the
 * service checks, because no single-field annotation can express "one or the other".
 */
public record SendMessageRequest(@Size(max = 10_000) String text, @Size(max = 1024) String imageUrl) {}
