# Payment Processor Application

A high-performance Java application that processes payment requests from Kafka and stores them in PostgreSQL.

## Features

- Processes payment requests from Kafka topic
- Stores payment data in PostgreSQL
- Uses virtual threads for high concurrency
- Configurable maximum concurrent requests
- Sends responses to caller-specific reply topics
- Database schema management with Liquibase
- Comprehensive validation of payment requests
- Error handling and logging

## Prerequisites

- Java 21 or later
- Maven 3.8 or later
- PostgreSQL 12 or later
- Kafka 3.6 or later

## Configuration

The application can be configured using environment variables or the `application.yml` file:

### Environment Variables
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: localhost:9092)
- `DB_URL`: PostgreSQL connection URL (default: jdbc:postgresql://localhost:5432/payments)
- `DB_USERNAME`: PostgreSQL username (default: postgres)
- `DB_PASSWORD`: PostgreSQL password (default: admin)

### Database Configuration
The application uses Liquibase for database schema management. Configuration is in `src/main/resources/liquibase/liquibase.properties`:

```properties
driver=org.postgresql.Driver
url=jdbc:postgresql://localhost:5432/postgres
username=postgres
password=admin
defaultSchemaName=payments
changeLogFile=src/main/resources/liquibase/changelog/master.xml
createDatabaseIfNotExist=true
```

## Building

```bash
mvn clean package
```

## Database Setup

### Using Liquibase Maven Plugin
To create and update the database schema:

```bash
mvn liquibase:update
```

To rollback changes:

```bash
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### Manual Database Setup
1. Create a PostgreSQL database:
```sql
CREATE DATABASE postgres;
```

2. Create the schema:
```sql
CREATE SCHEMA payments;
```

## Running the Application

### Development Mode
```bash
mvn spring-boot:run
```

### Production Mode
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
    "timestamp": "2024-04-09T12:00:00Z",
    "replyTopic": "payment-responses-{client-id}"
}
```

### Validation Rules
- `requestId`: Required, non-blank
- `payerAccountNumber`: Required, non-blank
- `paymentType`: Required, non-blank
- `amount`: Required, positive number
- `timestamp`: Required, valid ISO-8601 timestamp
- `replyTopic`: Required, non-blank

## Response Format

Responses will be sent to the specified reply topic in JSON format:

```json
{
    "status": "success",
    "requestId": "unique-request-id",
    "invoiceId": "INV-123456",
    "errorMessage": null
}
```

Error response format:
```json
{
    "status": "error",
    "requestId": "unique-request-id",
    "invoiceId": null,
    "errorMessage": "Validation error: requestId is required"
}
```

## Architecture

- Uses virtual threads for high concurrency
- Implements connection pooling for database access
- Uses a semaphore to limit concurrent requests
- Implements graceful shutdown
- Provides comprehensive logging
- Database schema versioning with Liquibase
- Input validation using Hibernate Validator
- JSON processing with Jackson

## Performance Considerations

- Maximum concurrent requests: 1000 (configurable)
- Uses HikariCP connection pool (20 max connections)
- Virtual threads for efficient request processing
- Asynchronous Kafka message processing
- Efficient JSON serialization/deserialization

## Testing

Run the test suite:
```bash
mvn test
```

The test suite includes:
- Unit tests for payment processing
- Integration tests for Kafka message handling
- Validation tests for payment requests
- Error handling tests

## Logging

Logging is configured in `application.yml`:
```yaml
logging:
  level:
    org.springframework: INFO
    com.payment: DEBUG
```

## Monitoring

The application includes Spring Boot Actuator endpoints for monitoring:
- Health check: `/actuator/health`
- Metrics: `/actuator/metrics`
- Info: `/actuator/info` 