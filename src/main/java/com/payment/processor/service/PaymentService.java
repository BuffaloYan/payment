package com.payment.processor.service;

import com.payment.processor.model.PaymentRequest;
import com.payment.processor.model.PaymentResponse;
import com.payment.processor.repository.PaymentRepository;
import com.payment.processor.validation.ValidationResult;
import com.payment.processor.validation.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    private final PaymentRepository repository;
    
    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }
    
    public PaymentResponse processPayment(PaymentRequest request) {
        logger.info("Processing payment request: {}", request);
        
        ValidationResult validationResult = ValidationUtils.validate(request);
        if (!validationResult.isValid()) {
            return PaymentResponse.builder()
                .status("error")
                .requestId(request.getRequestId())
                .errorMessage(validationResult.getErrorMessage())
                .build();
        }
        
        try {
            repository.savePaymentRequest(request);
            
            return PaymentResponse.builder()
                .status("success")
                .requestId(request.getRequestId())
                .invoiceId("INV-" + UUID.randomUUID().toString())
                .build();
        } catch (Exception e) {
            logger.error("Error processing payment request", e);
            return PaymentResponse.builder()
                .status("error")
                .requestId(request.getRequestId())
                .errorMessage("Internal error: " + e.getMessage())
                .build();
        }
    }
} 