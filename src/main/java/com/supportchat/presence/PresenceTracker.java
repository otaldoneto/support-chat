package com.supportchat.presence;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

// Tracks which sessions are watching which conversation. In-memory only: fine for a single-instance demo,
// would need a shared store (e.g. Redis) behind a load balancer with multiple instances.
@Controller
public class PresenceTracker {

    private final Map<String, Long> conversationBySession = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceTracker(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/conversations/{conversationId}/join")
    public void join(@DestinationVariable Long conversationId, SimpMessageHeaderAccessor headerAccessor) {
        conversationBySession.put(headerAccessor.getSessionId(), conversationId);
        broadcastCount(conversationId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Long conversationId = conversationBySession.remove(event.getSessionId());
        if (conversationId != null) {
            broadcastCount(conversationId);
        }
    }

    private void broadcastCount(Long conversationId) {
        long count = conversationBySession.values().stream().filter(conversationId::equals).count();
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/presence", count);
    }
}
