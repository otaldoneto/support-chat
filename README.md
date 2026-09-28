# Support Chat

A real-time support chat built with Java, Spring Boot, and WebSockets (STOMP). A customer opens a conversation, an
agent replies, and both see messages instantly — no polling, no page refresh. Backed by PostgreSQL for persistence.

This is a portfolio project, run as a local demo — see [Limitations](#limitations) below.

## Stack

- **Java 25** / **Spring Boot 4.1.1**
- **WebSocket + STOMP** — real-time, bidirectional messaging
- **PostgreSQL 18** — conversations and message history
- **Flyway** — database migrations
- **Testcontainers** — integration tests against a real PostgreSQL, no mocks

## Running it locally

Requires Docker and a JDK 25.

```bash
docker compose up -d
./mvnw spring-boot:run
```

Then open `http://localhost:8080` — a minimal demo page is served automatically. Open it in two browser tabs, type
the same conversation ID in both (see below for how to get one), and send messages back and forth.

### Try it

Start a conversation:
```bash
curl -X POST http://localhost:8080/api/conversations -H "Content-Type: application/json" -d '{"customerName":"Maria"}'
```

Use the returned `id` as the "Conversation ID" in the demo page (in both tabs). Messages sent from one tab appear
instantly in the other, and the page shows how many people are currently connected to that conversation.

### Running the tests

```bash
./mvnw test
```

Includes a full end-to-end test that connects two real STOMP clients over a real WebSocket connection (no mocks,
no browser) against a real PostgreSQL via Testcontainers.

## API

| Method | Path                          | Description                          |
|--------|-------------------------------|---------------------------------------|
| POST   | `/api/conversations`          | Start a conversation                  |
| GET    | `/api/conversations/{id}/messages` | Get the message history          |

## WebSocket (STOMP)

| Destination                                         | Direction        | Description                          |
|------------------------------------------------------|------------------|----------------------------------------|
| `/app/conversations/{id}/chat.send`                  | Client → server  | Send a message                        |
| `/app/conversations/{id}/join`                       | Client → server  | Register presence in a conversation   |
| `/topic/conversations/{id}`                          | Server → client  | New messages in that conversation     |
| `/topic/conversations/{id}/presence`                 | Server → client  | How many clients are connected        |

Connect at `/ws` (with SockJS, for browsers) or `/ws-native` (raw WebSocket, for non-browser clients).

## Technical decisions

**STOMP instead of raw WebSocket frames.** Raw WebSocket only gives you a byte/text pipe — every routing decision
("which conversation is this for?", "who should receive this?") would have to be invented from scratch. STOMP adds
a thin messaging protocol on top: named destinations (`/topic/...`, `/app/...`) and pub/sub semantics, which Spring
already knows how to wire to `@MessageMapping` methods and a broker — the same mental model as message queues, just
over a WebSocket connection instead of AMQP.

**Two STOMP endpoints, `/ws` and `/ws-native`.** The demo page (a browser) connects through SockJS, which falls
back to HTTP long-polling on networks or old browsers that block WebSocket — worth having for a page real users
open. A JVM client (this project's own integration test, or any other backend service that might talk to this one)
has no such limitation and talks raw WebSocket directly, so it uses a separate endpoint without the SockJS layer.
Mixing both behaviors on a single endpoint isn't supported — SockJS expects its own handshake sequence, and a plain
WebSocket upgrade request against it is rejected.

**Dynamic destinations via `SimpMessagingTemplate`, not `@SendTo`.** `@SendTo` binds a message handler to a fixed
destination decided at compile time — fine for a single global topic, but each conversation here needs its own
(`/topic/conversations/42`), known only at request time. `SimpMessagingTemplate.convertAndSend(...)` sends to any
destination computed at runtime.

**Presence tracked in memory, keyed by WebSocket session.** A `ConcurrentHashMap` maps each connected session ID to
the conversation it's watching, updated on `SessionDisconnectEvent` (fired automatically when a connection drops,
even an ungraceful one) and on an explicit `join` message per conversation. This is fine for a single instance; see
[Limitations](#limitations).

**An injectable `Clock` instead of `Instant.now()`.** Every timestamp (conversation creation, message send time)
goes through a `java.time.Clock` bean rather than calling `Instant.now()` directly, so tests can control time
without real delays — the same pattern used across this portfolio's other projects.

## Limitations

This is a local demo, not a production deployment:
- No authentication — anyone who can reach the server can join any conversation by guessing its ID.
- Presence tracking is in-memory and per-instance. Running more than one instance behind a load balancer would
  need a shared store (e.g. Redis, as used for rate limiting in this portfolio's
  [url-shortener](https://github.com/otaldoneto/url-shortener) project) so every instance sees the same presence
  state.
- The very first client to connect to a conversation never sees its own presence broadcast — the server announces
  a new connection before that connection's own subscription exists. Acceptable for a demo; a production version
  would send the current count directly to a newly connected client in addition to broadcasting to everyone else.

## CI

Every push and pull request against `main` runs the full test suite (including the Testcontainers-based integration
tests) via GitHub Actions — see [`.github/workflows/ci.yml`](.github/workflows/ci.yml).
