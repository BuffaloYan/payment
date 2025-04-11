# Payment Processor Application

A high-performance Java application that processes payment requests from Kafka and stores them in PostgreSQL.

## Features

- Processes payment requests from Kafka topic
- Stores payment data in PostgreSQL
- Uses virtual threads for high concurrency
- Configurable maximum concurrent requests
- Sends responses to caller-specific reply topics

## Prerequisites

- Java 21 or later
- Maven 3.8 or later
- PostgreSQL 12 or later
- Kafka 3.6 or later

## Configuration

The application can be configured using environment variables:

- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: localhost:9092)
- `DB_URL`: PostgreSQL connection URL (default: jdbc:postgresql://localhost:5432/payments)
- `DB_USERNAME`: PostgreSQL username (default: postgres)
- `DB_PASSWORD`: PostgreSQL password (default: postgres)

## Building

```bash
mvn clean package
```

## Running

```bash
java -jar target/payment-processor-1.0-SNAPSHOT.jar
```

## Payment Request Format

Payment requests should be sent to the `payment-requests` topic in JSON format:

```json
{
    "requestId": "unique-request-id",
    "payerAccountNumber": "payer-account-number",
    "paymentType": "payment-type",
    "amount": 100.00,
    "receiverAccountNumber": "receiver-account-number",
    "timestamp": "2024-04-09T12:00:00Z",
    "replyTopic": "payment-responses-{client-id}"
}
```

## Response Format

Responses will be sent to the specified reply topic in JSON format:

```json
{
    "status": "success",
    "requestId": "unique-request-id"
}
```

## Architecture

- Uses virtual threads for high concurrency
- Implements connection pooling for database access
- Uses a semaphore to limit concurrent requests
- Implements graceful shutdown
- Provides comprehensive logging

## Performance Considerations

- Maximum concurrent requests: 1000 (configurable)
- Uses HikariCP connection pool (20 max connections)
- Virtual threads for efficient request processing
- Asynchronous Kafka message processing 