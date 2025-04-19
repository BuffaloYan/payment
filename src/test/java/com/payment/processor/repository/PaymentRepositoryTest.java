package com.payment.processor.repository;

import com.payment.processor.model.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryTest {
    @Mock
    private JdbcTemplate jdbcTemplate;
    
    private PaymentRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new PaymentRepository(jdbcTemplate);
    }
    
    @Test
    void testSavePaymentRequest() {
        // Given
        Instant now = Instant.now();
        PaymentRequest request = PaymentRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .payerAccountNumber("PAYER123")
            .paymentType("TRANSFER")
            .amount(100.50)
            .timestamp(now)
            .replyTopic("test-reply-topic")
            .build();
        
        // When
        repository.savePaymentRequest(request);
        
        // Then
        verify(jdbcTemplate).update(
            eq("INSERT INTO payment_requests (request_id, payer_account_number, payment_type, amount, timestamp, reply_topic) VALUES (?, ?, ?, ?, ?, ?)"),
            eq(request.getRequestId()),
            eq(request.getPayerAccountNumber()),
            eq(request.getPaymentType()),
            eq(request.getAmount()),
            eq(Timestamp.from(now)),
            eq(request.getReplyTopic())
        );
    }
} 