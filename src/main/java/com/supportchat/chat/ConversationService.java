package com.supportchat.chat;

import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final Clock clock;

    public ConversationService(ConversationRepository conversationRepository, MessageRepository messageRepository,
                               Clock clock) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.clock = clock;
    }

    @Transactional
    public Conversation start(String customerName) {
        return conversationRepository.save(new Conversation(customerName, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Conversation find(Long id) {
        return conversationRepository.findById(id).orElseThrow(() -> new ConversationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Message> findMessages(Long conversationId) {
        find(conversationId); // 404s if the conversation does not exist, before returning an empty list
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    @Transactional
    public Message addMessage(Long conversationId, String sender, String content) {
        Conversation conversation = find(conversationId);
        return messageRepository.save(new Message(conversation, sender, content, clock.instant()));
    }
}
