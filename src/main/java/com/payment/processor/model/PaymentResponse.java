package com.payment.processor.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PaymentResponse {
    private String status;
    private String requestId;
    private String invoiceId;
    private String errorMessage;

    // Default constructor for Jackson
    public PaymentResponse() {
    }

    public PaymentResponse(String status, String requestId, String invoiceId, String errorMessage) {
        this.status = status;
        this.requestId = requestId;
        this.invoiceId = invoiceId;
        this.errorMessage = errorMessage;
    }
} 