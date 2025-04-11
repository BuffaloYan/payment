package com.payment.processor.repository;

import com.payment.processor.model.PaymentRequest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

public class PaymentRepository {
    private final HikariDataSource dataSource;

    public PaymentRepository(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        this.dataSource = new HikariDataSource(config);
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS payment_requests (
                request_id VARCHAR(36) PRIMARY KEY,
                payer_account_number VARCHAR(50) NOT NULL,
                payment_type VARCHAR(50) NOT NULL,
                amount DECIMAL(15,2) NOT NULL,
                receiver_account_number VARCHAR(50) NOT NULL,
                timestamp TIMESTAMP NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create payment_requests table", e);
        }
    }

    public void savePaymentRequest(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }
        
        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID is required");
        }
        
        if (request.getPayerAccountNumber() == null || request.getPayerAccountNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Payer account number is required");
        }
        
        if (request.getPaymentType() == null || request.getPaymentType().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment type is required");
        }
        
        if (request.getReceiverAccountNumber() == null || request.getReceiverAccountNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Receiver account number is required");
        }

        String sql = """
            INSERT INTO payment_requests 
            (request_id, payer_account_number, payment_type, amount, receiver_account_number, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, request.getRequestId());
            stmt.setString(2, request.getPayerAccountNumber());
            stmt.setString(3, request.getPaymentType());
            stmt.setDouble(4, request.getAmount());
            stmt.setString(5, request.getReceiverAccountNumber());
            
            // Handle null timestamp by using current timestamp
            Instant timestamp = request.getTimestamp();
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            stmt.setTimestamp(6, java.sql.Timestamp.from(timestamp));
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payment request: " + e.getMessage(), e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
} 