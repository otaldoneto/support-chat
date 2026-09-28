package com.supportchat.chat;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ConversationService conversationService, SimpMessagingTemplate messagingTemplate) {
        this.conversationService = conversationService;
        this.messagingTemplate = messagingTemplate;
    }

    // A client sends to "/app/conversations/{conversationId}/chat.send"; the message is saved, then broadcast
    // only to whoever is subscribed to that specific conversation's topic.
    @MessageMapping("/conversations/{conversationId}/chat.send")
    public void send(@DestinationVariable Long conversationId, ChatMessageRequest request) {
        Message saved = conversationService.addMessage(conversationId, request.sender(), request.content());
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, MessageResponse.from(saved));
    }
}
