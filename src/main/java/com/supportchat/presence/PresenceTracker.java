package com.supportchat.presence;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

// Tracks which WebSocket sessions are currently connected. In-memory only: fine for a single-instance demo,
// would need a shared store (e.g. Redis) if this ran behind a load balancer with multiple instances.
@Component
public class PresenceTracker {

    private final Set<String> connectedSessions = ConcurrentHashMap.newKeySet();
    private final SimpMessagingTemplate messagingTemplate;


    public PresenceTracker(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;

    }

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        connectedSessions.add(accessor.getSessionId());
        broadcastCount();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        connectedSessions.remove(event.getSessionId());
        broadcastCount();
    }

    private void broadcastCount() {
        messagingTemplate.convertAndSend("/topic/presence", connectedSessions.size());
    }
}
