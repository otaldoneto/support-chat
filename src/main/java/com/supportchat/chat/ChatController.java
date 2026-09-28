package com.supportchat.chat;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    // A client sends to "/app/chat.send" (the "/app" prefix comes from configureMessageBroker);
    // the return value is broadcast to everyone subscribed to "/topic/messages"
    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessage send(ChatMessage message) {
        return message;
    }
}
