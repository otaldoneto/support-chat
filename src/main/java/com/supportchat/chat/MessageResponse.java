package com.supportchat.chat;

import java.time.Instant;

public record MessageResponse(Long id, String sender, String content, Instant sentAt) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(message.getId(), message.getSender(), message.getContent(), message.getSentAt());
    }
}
