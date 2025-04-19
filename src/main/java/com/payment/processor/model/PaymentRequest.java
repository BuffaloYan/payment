package com.payment.processor.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotBlank(message = "Request ID is required")
    private String requestId;

    @NotBlank(message = "Payer account number is required")
    private String payerAccountNumber;

    @NotBlank(message = "Payment type is required")
    private String paymentType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    @NotBlank(message = "Reply topic is required")
    private String replyTopic;
} 