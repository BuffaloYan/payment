package com.payment.processor.repository;

import com.payment.processor.model.PaymentRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;

@Repository
public class PaymentRepository {
    private final JdbcTemplate jdbcTemplate;
    
    public PaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Transactional
    public void savePaymentRequest(PaymentRequest request) {
        String sql = "INSERT INTO payment_requests (request_id, payer_account_number, payment_type, amount, timestamp, reply_topic) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
            request.getRequestId(),
            request.getPayerAccountNumber(),
            request.getPaymentType(),
            request.getAmount(),
            Timestamp.from(request.getTimestamp()),
            request.getReplyTopic()
        );
    }
    
    public void close() {
        // No-op as Spring manages the JdbcTemplate lifecycle
    }
} 