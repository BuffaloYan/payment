package com.payment.processor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payment.processor.model.PaymentRequest;
import com.payment.processor.model.PaymentResponse;
import com.payment.processor.repository.PaymentRepository;
import com.payment.processor.validation.ValidationResult;
import com.payment.processor.validation.ValidationUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaPaymentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(KafkaPaymentProcessor.class);
    private static final int MAX_CONCURRENT_REQUESTS = 1000;
    private static final String PAYMENT_TOPIC = "payment-requests";
    
    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> producer;
    private final PaymentRepository repository;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final Semaphore concurrentRequests;
    private final AtomicBoolean running;
    private final String bootstrapServers;

    public KafkaPaymentProcessor(String bootstrapServers, PaymentRepository repository) {
        this.bootstrapServers = bootstrapServers;
        this.repository = repository;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.concurrentRequests = new Semaphore(MAX_CONCURRENT_REQUESTS);
        this.running = new AtomicBoolean(true);
        this.consumer = createConsumer();
        this.producer = createProducer();
    }

    protected KafkaConsumer<String, String> createConsumer() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-processor-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new KafkaConsumer<>(consumerProps);
    }

    protected KafkaProducer<String, String> createProducer() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(producerProps);
    }

    public void start() {
        consumer.subscribe(Collections.singletonList(PAYMENT_TOPIC));
        
        while (running.get()) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                
                for (ConsumerRecord<String, String> record : records) {
                    if (concurrentRequests.tryAcquire()) {
                        executorService.submit(() -> {
                            try {
                                processPaymentRequest(record);
                            } finally {
                                concurrentRequests.release();
                            }
                        });
                    } else {
                        logger.warn("Maximum concurrent requests reached, skipping message");
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing messages", e);
            }
        }
    }

    private void processPaymentRequest(ConsumerRecord<String, String> record) {
        try {
            PaymentRequest request = objectMapper.readValue(record.value(), PaymentRequest.class);
            
            // Validate request using Hibernate Validator
            ValidationResult validationResult = ValidationUtils.validate(request);
            if (!validationResult.isValid()) {
                sendErrorResponse(request, validationResult.getErrorMessage());
                return;
            }
            
            logger.info("Processing payment request: {}", request.getRequestId());
            
            // Save to database
            repository.savePaymentRequest(request);
            
            // Generate invoice ID
            String invoiceId = generateInvoiceId(request);
            
            // Create and send success response
            PaymentResponse response = PaymentResponse.builder()
                .status("success")
                .requestId(request.getRequestId())
                .invoiceId(invoiceId)
                .build();
            
            String responseJson = objectMapper.writeValueAsString(response);
            ProducerRecord<String, String> responseRecord = new ProducerRecord<>(
                request.getReplyTopic(),
                request.getRequestId(),
                responseJson
            );
            producer.send(responseRecord);
            
            logger.info("Successfully processed payment request: {}, invoiceId: {}", 
                request.getRequestId(), invoiceId);
        } catch (Exception e) {
            logger.error("Error processing payment request", e);
        }
    }

    private void sendErrorResponse(PaymentRequest request, String errorMessage) {
        try {
            PaymentResponse response = PaymentResponse.builder()
                .status("error")
                .requestId(request.getRequestId() != null ? request.getRequestId() : "UNKNOWN")
                .errorMessage(errorMessage)
                .build();
            
            String responseJson = objectMapper.writeValueAsString(response);
            ProducerRecord<String, String> responseRecord = new ProducerRecord<>(
                request.getReplyTopic() != null ? request.getReplyTopic() : "payment-responses",
                request.getRequestId() != null ? request.getRequestId() : "UNKNOWN",
                responseJson
            );
            producer.send(responseRecord);
            
            logger.error("Validation error processing payment request: {}", errorMessage);
        } catch (Exception e) {
            logger.error("Error sending error response", e);
        }
    }

    private String generateInvoiceId(PaymentRequest request) {
        // Generate a unique invoice ID based on request details
        // Format: INV-{timestamp}-{hash}
        String timestamp = String.valueOf(System.currentTimeMillis());
        String hash = UUID.randomUUID().toString().substring(0, 8);
        return String.format("INV-%s-%s", timestamp, hash);
    }

    public void shutdown() {
        running.set(false);
        executorService.shutdown();
        consumer.close();
        producer.close();
        repository.close();
    }
} 