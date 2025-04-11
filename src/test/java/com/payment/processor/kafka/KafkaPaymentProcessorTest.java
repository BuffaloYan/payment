package com.payment.processor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payment.processor.model.PaymentRequest;
import com.payment.processor.model.PaymentResponse;
import com.payment.processor.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaPaymentProcessorTest {
    @Mock
    private KafkaConsumer<String, String> consumer;
    
    @Mock
    private KafkaProducer<String, String> producer;
    
    @Mock
    private PaymentRepository repository;
    
    private KafkaPaymentProcessor processor;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final String bootstrapServers = "localhost:9092";

    @BeforeEach
    void setUp() {
        processor = new KafkaPaymentProcessor(bootstrapServers, repository) {
            @Override
            protected KafkaConsumer<String, String> createConsumer() {
                return consumer;
            }
            
            @Override
            protected KafkaProducer<String, String> createProducer() {
                return producer;
            }
        };
    }

    @Test
    void testProcessPaymentRequest() throws Exception {
        // Prepare test data
        String requestId = UUID.randomUUID().toString();
        PaymentRequest request = PaymentRequest.builder()
            .requestId(requestId)
            .payerAccountNumber("PAYER123")
            .paymentType("TRANSFER")
            .amount(100.50)
            .receiverAccountNumber("RECEIVER456")
            .timestamp(Instant.now())
            .replyTopic("payment-responses")
            .build();

        String requestJson = objectMapper.writeValueAsString(request);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("payment-requests", 0, 0, requestId, requestJson);

        // Mock consumer.poll() to return our test record for first call and empty records after
        ConsumerRecords<String, String> records = new ConsumerRecords<>(Collections.singletonMap(
            new org.apache.kafka.common.TopicPartition("payment-requests", 0),
            Collections.singletonList(record)
        ));
        ConsumerRecords<String, String> emptyRecords = new ConsumerRecords<>(Collections.emptyMap());
        
        // First call returns records, subsequent calls return empty records
        when(consumer.poll(any()))
            .thenReturn(records)
            .thenReturn(emptyRecords);

        // Start processing in a separate thread
        Thread processorThread = new Thread(() -> {
            try {
                processor.start();
            } catch (Exception e) {
                fail("Processor failed to start", e);
            }
        });
        processorThread.start();

        // Wait a bit for processing
        Thread.sleep(100);

        // Verify repository was called exactly once
        verify(repository, times(1)).savePaymentRequest(request);

        // Verify producer was called exactly once with correct response
        ArgumentCaptor<ProducerRecord<String, String>> responseCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer, times(1)).send(responseCaptor.capture());

        ProducerRecord<String, String> sentRecord = responseCaptor.getValue();
        assertNotNull(sentRecord);
        assertEquals(request.getReplyTopic(), sentRecord.topic());
        assertEquals(requestId, sentRecord.key());

        PaymentResponse response = objectMapper.readValue(sentRecord.value(), PaymentResponse.class);
        assertEquals("success", response.getStatus());
        assertEquals(requestId, response.getRequestId());
        assertNotNull(response.getInvoiceId());
        assertTrue(response.getInvoiceId().startsWith("INV-"));

        // Cleanup
        processor.shutdown();
        processorThread.join(1000);
    }

    @Test
    void testProcessInvalidPaymentRequest() throws Exception {
        // Prepare invalid test data
        String invalidJson = "invalid-json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("payment-requests", 0, 0, "key", invalidJson);

        // Mock consumer.poll() to return our test record for first call and empty records after
        ConsumerRecords<String, String> records = new ConsumerRecords<>(Collections.singletonMap(
            new org.apache.kafka.common.TopicPartition("payment-requests", 0),
            Collections.singletonList(record)
        ));
        ConsumerRecords<String, String> emptyRecords = new ConsumerRecords<>(Collections.emptyMap());
        
        // First call returns records, subsequent calls return empty records
        when(consumer.poll(any()))
            .thenReturn(records)
            .thenReturn(emptyRecords);

        // Start processing in a separate thread
        Thread processorThread = new Thread(() -> {
            try {
                processor.start();
            } catch (Exception e) {
                fail("Processor failed to start", e);
            }
        });
        processorThread.start();

        // Wait a bit for processing
        Thread.sleep(100);

        // Verify repository was never called
        verify(repository, never()).savePaymentRequest(any());

        // Verify producer was never called
        verify(producer, never()).send(any());

        // Cleanup
        processor.shutdown();
        processorThread.join(1000);
    }

    @Test
    void testProcessPaymentRequestWithValidationError() throws Exception {
        // Prepare test data with missing required fields
        PaymentRequest request = PaymentRequest.builder()
            .requestId(null)  // Missing required field
            .payerAccountNumber("PAYER123")
            .paymentType("TRANSFER")
            .amount(100.50)
            .receiverAccountNumber("RECEIVER456")
            .timestamp(Instant.now())
            .replyTopic("payment-responses")
            .build();

        String requestJson = objectMapper.writeValueAsString(request);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("payment-requests", 0, 0, "key", requestJson);

        // Mock consumer.poll() to return our test record for first call and empty records after
        ConsumerRecords<String, String> records = new ConsumerRecords<>(Collections.singletonMap(
            new org.apache.kafka.common.TopicPartition("payment-requests", 0),
            Collections.singletonList(record)
        ));
        ConsumerRecords<String, String> emptyRecords = new ConsumerRecords<>(Collections.emptyMap());
        
        // First call returns records, subsequent calls return empty records
        when(consumer.poll(any()))
            .thenReturn(records)
            .thenReturn(emptyRecords);

        // Start processing in a separate thread
        Thread processorThread = new Thread(() -> {
            try {
                processor.start();
            } catch (Exception e) {
                fail("Processor failed to start", e);
            }
        });
        processorThread.start();

        // Wait a bit for processing
        Thread.sleep(100);

        // Verify repository was never called
        verify(repository, never()).savePaymentRequest(any());

        // Verify producer was called exactly once with error response
        ArgumentCaptor<ProducerRecord<String, String>> responseCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer, times(1)).send(responseCaptor.capture());

        ProducerRecord<String, String> sentRecord = responseCaptor.getValue();
        assertNotNull(sentRecord);
        assertEquals(request.getReplyTopic(), sentRecord.topic());
        assertEquals("UNKNOWN", sentRecord.key());  // Key should be UNKNOWN since requestId is null

        PaymentResponse response = objectMapper.readValue(sentRecord.value(), PaymentResponse.class);
        assertEquals("error", response.getStatus());
        assertEquals("UNKNOWN", response.getRequestId());  // RequestId should be UNKNOWN
        assertEquals("Request ID is required", response.getErrorMessage());
        assertNull(response.getInvoiceId());

        // Cleanup
        processor.shutdown();
        processorThread.join(1000);
    }
} 