package com.supportchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages sent to "/topic/..." are broadcast to every subscriber by the broker
        registry.enableSimpleBroker("/topic");
        // Messages the client sends to "/app/..." are routed to an @MessageMapping method
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Used by the browser demo page: SockJS falls back to long-polling in old browsers/proxies that block WebSocket
        registry.addEndpoint("/ws").withSockJS();
        // Used by non-browser clients (e.g. this project's own integration tests): raw WebSocket, no SockJS
        registry.addEndpoint("/ws-native");
    }
}
