package com.supportchat.chat;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(Long id) {
        super("No conversation with id " + id);
    }
}
