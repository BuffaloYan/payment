package com.payment.processor.repository;

import com.payment.processor.model.PaymentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentRepositoryTest {
    private PaymentRepository repository;
    private static final String TEST_DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String TEST_USERNAME = "sa";
    private static final String TEST_PASSWORD = "";

    @BeforeEach
    void setUp() {
        repository = new PaymentRepository(TEST_DB_URL, TEST_USERNAME, TEST_PASSWORD);
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void testSavePaymentRequest() {
        PaymentRequest request = PaymentRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .payerAccountNumber("PAYER123")
            .paymentType("TRANSFER")
            .amount(100.50)
            .timestamp(Instant.now())
            .replyTopic("payment-responses")
            .build();

        assertDoesNotThrow(() -> repository.savePaymentRequest(request));
    }

    @Test
    void testSavePaymentRequestWithNullValues() {
        PaymentRequest request = PaymentRequest.builder()
            .requestId(null)
            .payerAccountNumber(null)
            .paymentType(null)
            .amount(0.0)
            .timestamp(null)
            .replyTopic(null)
            .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> repository.savePaymentRequest(request));
        assertEquals("Request ID is required", exception.getMessage());
    }
} 