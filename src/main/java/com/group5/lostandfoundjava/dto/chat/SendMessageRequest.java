package com.group5.lostandfoundjava.dto.chat;

import jakarta.validation.constraints.Size;


public record SendMessageRequest(@Size(max = 10_000) String text, @Size(max = 1024) String imageUrl) {}
