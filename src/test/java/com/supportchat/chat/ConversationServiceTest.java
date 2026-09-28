package com.supportchat.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T12:00:00Z");

    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final ConversationService service = new ConversationService(conversationRepository, messageRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void saveReturnsTheEntity() {
        when(conversationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void startsAConversationAsOpen() {
        Conversation conversation = service.start("Maria");

        assertThat(conversation.getCustomerName()).isEqualTo("Maria");
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.OPEN);
        assertThat(conversation.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void findThrowsWhenTheConversationDoesNotExist() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.find(1L)).isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void findMessagesThrowsWhenTheConversationDoesNotExistAndNeverQueriesMessages() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findMessages(1L)).isInstanceOf(ConversationNotFoundException.class);
        verify(messageRepository, never()).findByConversationIdOrderBySentAtAsc(any());
    }

    @Test
    void findMessagesReturnsThemInOrder() {
        Conversation conversation = new Conversation("Maria", NOW);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        Message first = new Message(conversation, "Maria", "Hi", NOW);
        when(messageRepository.findByConversationIdOrderBySentAtAsc(1L)).thenReturn(List.of(first));

        List<Message> messages = service.findMessages(1L);

        assertThat(messages).containsExactly(first);
    }

    @Test
    void addMessageSavesItUnderTheConversation() {
        Conversation conversation = new Conversation("Maria", NOW);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        Message saved = service.addMessage(1L, "Maria", "Hello");

        assertThat(saved.getConversation()).isEqualTo(conversation);
        assertThat(saved.getSender()).isEqualTo("Maria");
        assertThat(saved.getContent()).isEqualTo("Hello");
        assertThat(saved.getSentAt()).isEqualTo(NOW);
    }

    @Test
    void addMessageThrowsWhenTheConversationDoesNotExist() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addMessage(1L, "Maria", "Hello"))
                .isInstanceOf(ConversationNotFoundException.class);
        verify(messageRepository, never()).save(any());
    }
}
