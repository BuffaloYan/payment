package com.payment.processor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.processor.model.PaymentRequest;
import com.payment.processor.model.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class KafkaPaymentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(KafkaPaymentProcessor.class);
    
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    public KafkaPaymentProcessor(
            ObjectMapper objectMapper,
            PaymentService paymentService,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${spring.kafka.topics.payment-requests}", groupId = "${spring.kafka.group-id}")
    public void processPaymentRequest(@Payload String message) {
        PaymentRequest request = null;
        try {
            logger.info("Received raw message: {}", message);
            request = objectMapper.readValue(message, PaymentRequest.class);
            logger.info("Deserialized payment request: {}", request);
            
            final PaymentRequest finalRequest = request;
            PaymentResponse response = paymentService.processPayment(finalRequest);
            logger.info("Payment service response: {}", response);
            
            String responseJson = objectMapper.writeValueAsString(response);
            logger.info("Serialized response JSON: {}", responseJson);
            
            logger.info("Sending payment response to topic {}: {}", finalRequest.getReplyTopic(), response);
            kafkaTemplate.send(finalRequest.getReplyTopic(), responseJson).whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send response to topic {}: {}", finalRequest.getReplyTopic(), ex.getMessage());
                } else {
                    logger.info("Successfully sent response to topic {}", finalRequest.getReplyTopic());
                }
            });
        } catch (Exception e) {
            logger.error("Error processing payment request. Message: {}, Error: {}", message, e.getMessage(), e);
            if (request != null) {
                final PaymentRequest finalRequest = request;
                try {
                    PaymentResponse errorResponse = PaymentResponse.builder()
                        .requestId(finalRequest.getRequestId())
                        .status("error")
                        .errorMessage("Failed to process payment: " + e.getMessage())
                        .build();
                    String errorJson = objectMapper.writeValueAsString(errorResponse);
                    logger.info("Sending error response: {}", errorJson);
                    kafkaTemplate.send(finalRequest.getReplyTopic(), errorJson).whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to send error response to topic {}: {}", finalRequest.getReplyTopic(), ex.getMessage());
                        } else {
                            logger.info("Successfully sent error response to topic {}", finalRequest.getReplyTopic());
                        }
                    });
                } catch (Exception ex) {
                    logger.error("Failed to send error response: {}", ex.getMessage(), ex);
                }
            }
        }
    }
} 