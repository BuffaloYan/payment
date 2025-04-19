package com.payment.processor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.processor.config.TestConfig;
import com.payment.processor.model.PaymentRequest;
import com.payment.processor.model.PaymentResponse;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = TestConfig.class)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, 
    brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
    topics = {"test-payment-requests", "test-payment-responses-1", "test-payment-responses-2"})
public class KafkaPaymentProcessorTest {

    private static final String PAYMENT_TOPIC = "test-payment-requests";
    private static final String REPLY_TOPIC_1 = "test-payment-responses-1";
    private static final String REPLY_TOPIC_2 = "test-payment-responses-2";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> consumer;
    private String currentReplyTopic;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> "${spring.embedded.kafka.brokers}");
        registry.add("spring.kafka.consumer.group-id", () -> "test-payment-processor-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    private void setupConsumer(String replyTopic) {
        if (consumer != null) {
            consumer.unsubscribe();
            consumer.close();
        }
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "test-payment-processor-group-" + UUID.randomUUID(), 
            "true", 
            embeddedKafkaBroker
        );
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList(replyTopic));
        consumer.poll(Duration.ofMillis(100)); // Clear any existing messages
        currentReplyTopic = replyTopic;
    }

    @BeforeEach
    void setUp() {
        // Consumer will be set up per test
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.unsubscribe();
            consumer.close();
        }
    }

    @Test
    void testProcessPaymentRequest() throws Exception {
        setupConsumer(REPLY_TOPIC_1);

        // Create a valid payment request
        PaymentRequest request = PaymentRequest.builder()
                .requestId("test-request-1")
                .payerAccountNumber("123456789")
                .paymentType("CREDIT")
                .amount(100.0)
                .timestamp(Instant.now())
                .replyTopic(REPLY_TOPIC_1)
                .build();

        // Convert request to JSON and send
        String jsonRequest = objectMapper.writeValueAsString(request);
        System.out.println("Sending payment request: " + jsonRequest);
        kafkaTemplate.send(PAYMENT_TOPIC, jsonRequest).get(); // Wait for send completion
        System.out.println("Payment request sent successfully");

        // Poll for the response
        System.out.println("Waiting for response on topic: " + REPLY_TOPIC_1);
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, REPLY_TOPIC_1, Duration.ofSeconds(10));
        assertNotNull(record, "No response received within timeout period");
        System.out.println("Received response: " + record.value());
        
        PaymentResponse response = objectMapper.readValue(record.value(), PaymentResponse.class);
        System.out.println("Deserialized response: " + response);
        
        assertEquals("success", response.getStatus().toLowerCase(), 
            "Expected status 'success' but got '" + response.getStatus() + "' with error message: " + response.getErrorMessage());
        assertEquals(request.getRequestId(), response.getRequestId(), 
            "Response request ID does not match original request ID");
    }

    @Test
    void testProcessPaymentRequestWithValidationError() throws Exception {
        setupConsumer(REPLY_TOPIC_2);

        // Create an invalid payment request (missing required fields)
        PaymentRequest request = PaymentRequest.builder()
                .requestId("test-request-2")
                .paymentType("CREDIT")
                .amount(100.0)
                .timestamp(Instant.now())
                .replyTopic(REPLY_TOPIC_2)
                .build(); // Missing payerAccountNumber

        // Convert request to JSON and send
        String jsonRequest = objectMapper.writeValueAsString(request);
        System.out.println("Sending invalid payment request: " + jsonRequest);
        kafkaTemplate.send(PAYMENT_TOPIC, jsonRequest).get(); // Wait for send completion
        System.out.println("Invalid payment request sent successfully");

        // Poll for the response
        System.out.println("Waiting for error response on topic: " + REPLY_TOPIC_2);
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, REPLY_TOPIC_2, Duration.ofSeconds(10));
        assertNotNull(record, "No error response received within timeout period");
        System.out.println("Received error response: " + record.value());
        
        PaymentResponse response = objectMapper.readValue(record.value(), PaymentResponse.class);
        System.out.println("Deserialized error response: " + response);
        
        assertEquals("error", response.getStatus().toLowerCase(), 
            "Expected status 'error' but got '" + response.getStatus() + "'");
        assertTrue(response.getErrorMessage().contains("Payer account number is required"), 
            "Error message should contain 'Payer account number is required' but got: " + response.getErrorMessage());
    }
} 