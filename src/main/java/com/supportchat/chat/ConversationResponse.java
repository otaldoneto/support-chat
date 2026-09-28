package com.supportchat.chat;

import java.time.Instant;

public record ConversationResponse(Long id, String customerName, ConversationStatus status, Instant createdAt) {

    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(conversation.getId(), conversation.getCustomerName(),
                conversation.getStatus(), conversation.getCreatedAt());
    }
}
