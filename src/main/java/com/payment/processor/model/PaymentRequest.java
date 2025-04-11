package com.payment.processor.model;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;

@Data
@Builder
public class PaymentRequest {
    private String requestId;
    private String payerAccountNumber;
    private String paymentType;
    private double amount;
    private String receiverAccountNumber;
    private Instant timestamp;
    private String replyTopic;

    // Default constructor for Jackson
    public PaymentRequest() {
    }

    public PaymentRequest(String requestId, String payerAccountNumber, String paymentType, double amount, 
                         String receiverAccountNumber, Instant timestamp, String replyTopic) {
        this.requestId = requestId;
        this.payerAccountNumber = payerAccountNumber;
        this.paymentType = paymentType;
        this.amount = amount;
        this.receiverAccountNumber = receiverAccountNumber;
        this.timestamp = timestamp;
        this.replyTopic = replyTopic;
    }
} 