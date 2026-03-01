# Sports Betting Settlement Service

A Spring Boot application demonstrating event-driven architecture for sports betting settlement using Kafka and RabbitMQ.

RocketMQ has known issues while running on Mac Arm processors.\
For simplicity's sake, we're using RabbitMQ instead, which has similar features.

## Prerequisites

- Java 25+
- Docker & Docker Compose
- Maven 3.9+

## Quick Start

### 1. Start Infrastructure

In a separate terminal, run:

```bash
docker compose up
```

This starts:
- **Kafka** (KRaft mode) on port 9092
- **Kafka UI** at http://localhost:8083
- **RabbitMQ** on port 5672 (AMQP) and 15672 (Management UI)

### 2. Verify RabbitMQ is Running

Open http://localhost:15672 and login with:
- Username: `guest`
- Password: `guest`

### 3. Run the Application

```bash
mvn spring-boot:run
```

### 4. Verify Setup

Open H2 Console at http://localhost:8080/h2-console:
- JDBC URL: `jdbc:h2:mem:betsdb`
- Username: `sa`
- Password: (empty)

Query seed data:
```sql
SELECT * FROM bets;
```

You should see 3 pending bets.

## API Usage

See [`http/api-requests.http`](http/api-requests.http) for sample requests
(works with IntelliJ HTTP Client or VS Code REST Client).

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   REST API      │     │     Kafka       │     │    RabbitMQ     │     │    RabbitMQ     │
│  POST /outcome  │────▶│ event-outcomes  │────▶│    Exchange     │────▶│      Queue      │
│                 │     │     (topic)     │     │ bet.settlements │     │ bet-settlements │
└─────────────────┘     └─────────────────┘     └─────────────────┘     └─────────────────┘
                                                                                │
                                                                                ▼
                                                                       ┌─────────────────┐
                                                                       │   Settlement    │
                                                                       │    Handler      │
                                                                       │  (H2 Database)  │
                                                                       └─────────────────┘
```

### Flow

1. **Publish Event Outcome** - External system POSTs event outcome to REST API
2. **Kafka Consumer** - Listens for event outcomes, queries pending bets, determines WIN/LOSS
3. **RabbitMQ Producer** - Publishes settlement messages to `bet.settlements` exchange
4. **RabbitMQ Exchange** - Routes messages to `bet-settlements-queue` via routing key
5. **RabbitMQ Consumer** - Processes settlements with idempotent updates to database

### Features

| Feature                   | Location                             | Description                                             |
|---------------------------|--------------------------------------|---------------------------------------------------------|
| Publish Event Outcome     | `features/publisheventoutcome/`      | REST endpoint to publish event outcomes to Kafka        |
| Match & Queue Settlements | `features/matchandqueuesettlements/` | Kafka listener that matches bets and queues settlements |
| Process Bet Settlement    | `features/processbetsettlement/`     | RabbitMQ consumer that settles bets in database         |

## Design Decisions

### Dual Message Broker Architecture

- **Kafka** for event ingestion - provides durable, ordered event streams
- **RabbitMQ** for settlement processing - provides built-in retry, dead-letter queues, and reliable message delivery

### Idempotent Settlement

The `settleIfPending` repository method uses conditional UPDATE:
```sql
UPDATE bet SET status = ?, settled_at = ? WHERE id = ? AND status = 'PENDING'
```

Returns affected rows (0 or 1), ensuring duplicate messages don't re-settle bets.

### UUIDv7 for Primary Keys

Uses time-ordered UUIDs (UUIDv7) via custom Hibernate generator:
- Globally unique without coordination
- Sortable by creation time
- Better index performance than random UUIDs

### Virtual Threads

Enabled via `spring.threads.virtual.enabled=true` for efficient blocking I/O handling.

## Assumptions

For the scope of this exercise:

- **Event Market ID is metadata only** — The `eventMarketId` field is stored for context but not used in settlement logic. All bets for a given `eventId` are settled together regardless of market.
- **Event Name is metadata only** — The provided `eventName` field in outcome isn't used for the settlement logic.
- **Event ID is safe for deduplication** — The `eventId` is assumed to be globally unique and can be used as an idempotency key for message deduplication (e.g., Kafka consumer offset tracking, duplicate event detection).
- **Binary outcomes only** — Bets are settled as WIN or LOSS. No support for draws, voids, refunds, or partial settlements.
- **Bets exist before outcomes arrive** — The system queries for existing pending bets when an outcome is received. Bets placed after an event outcome is published will not be settled.
- **Bet amounts are informational** — Amounts are stored and passed through the pipeline but no payout calculation is performed (no odds, no winnings computed).

## Considerations

### Performance

The current implementation uses **single-message Kafka consumption**: one event outcome → one DB query → N settlement
messages. This is simple and readable, but has a known scaling limitation.

**Scaling path:** For high-throughput scenarios, switch to batch message consumption and bulk DB queries.\
This reduces DB round-trips from N to 1 per batch, significantly improving throughput under load.

### Why UUIDv7?

Random UUIDs (v4) cause **B-tree index fragmentation** — each insert lands at a random page, causing splits and poor 
cache locality. UUIDv7 encodes a timestamp prefix, making IDs **time-ordered** and inserts sequential. This provides:

- Better insert performance (sequential page writes)
- Range queries on ID implicitly sort by creation time
- No coordination required (unlike auto-increment across nodes)

## Development

### Format Code

```bash
mvn spotless:apply
```
