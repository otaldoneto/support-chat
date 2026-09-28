package com.supportchat.chat;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> start(@Valid @RequestBody StartConversationRequest request) {
        Conversation conversation = service.start(request.customerName());
        ConversationResponse response = ConversationResponse.from(conversation);
        return ResponseEntity.created(URI.create("/api/conversations/" + conversation.getId())).body(response);
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@PathVariable Long id) {
        return service.findMessages(id).stream().map(MessageResponse::from).toList();
    }
}
