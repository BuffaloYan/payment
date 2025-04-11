package com.payment.processor.model;

import lombok.Data;
import java.time.Instant;

@Data
public class PaymentRequest {
    private String requestId;
    private String payerAccountNumber;
    private String paymentType;
    private double amount;
    private String receiverAccountNumber;
    private Instant timestamp;
    private String replyTopic;
} 