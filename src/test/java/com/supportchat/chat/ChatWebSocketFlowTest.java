package com.supportchat.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportchat.TestcontainersConfiguration;
import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

// End to end: two real STOMP clients (simulating the customer and the agent) exchange a message over a
// real WebSocket connection, against a real PostgreSQL (Testcontainers)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class ChatWebSocketFlowTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @Test
    void twoClientsExchangeMessagesInTheSameConversation() throws Exception {
        Long conversationId = startConversation("Maria");
        BlockingQueue<MessageResponse> customerInbox = new LinkedBlockingQueue<>();
        BlockingQueue<MessageResponse> agentInbox = new LinkedBlockingQueue<>();

        StompSession customerSession = connect();
        StompSession agentSession = connect();
        subscribeToMessages(customerSession, conversationId, customerInbox);
        subscribeToMessages(agentSession, conversationId, agentInbox);

        customerSession.send("/app/conversations/" + conversationId + "/chat.send",
                new ChatMessageRequest("Maria", "I need help"));

        MessageResponse receivedByAgent = agentInbox.poll(5, TimeUnit.SECONDS);
        assertThat(receivedByAgent).isNotNull();
        assertThat(receivedByAgent.sender()).isEqualTo("Maria");
        assertThat(receivedByAgent.content()).isEqualTo("I need help");

        // The sender also gets its own message back, since everyone subscribed to the topic receives it
        MessageResponse receivedByCustomer = customerInbox.poll(5, TimeUnit.SECONDS);
        assertThat(receivedByCustomer).isEqualTo(receivedByAgent);
    }

    private Long startConversation(String customerName) {
        ResponseEntity<ConversationResponse> response = restTemplate.postForEntity("/api/conversations",
                new StartConversationRequest(customerName), ConversationResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().id();
    }

    private StompSession connect() throws Exception {
        return stompClient.connectAsync("ws://localhost:" + port + "/ws-native", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    private void subscribeToMessages(StompSession session, Long conversationId,
                                     BlockingQueue<MessageResponse> inbox) {
        session.subscribe("/topic/conversations/" + conversationId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.add((MessageResponse) payload);
            }
        });
    }
}
