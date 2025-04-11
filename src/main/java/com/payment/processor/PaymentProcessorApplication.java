package com.payment.processor;

import com.payment.processor.kafka.KafkaPaymentProcessor;
import com.payment.processor.repository.PaymentRepository;
import com.payment.processor.validation.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentProcessorApplication {
    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessorApplication.class);
    
    public static void main(String[] args) {
        // Configuration - in a real application, these would come from environment variables or config files
        String kafkaBootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/payments");
        String dbUsername = System.getenv().getOrDefault("DB_USERNAME", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        try {
            // Initialize repository
            PaymentRepository repository = new PaymentRepository(dbUrl, dbUsername, dbPassword);
            
            // Initialize and start Kafka processor
            KafkaPaymentProcessor processor = new KafkaPaymentProcessor(kafkaBootstrapServers, repository);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down payment processor...");
                processor.shutdown();
                ValidationUtils.close();
            }));
            
            // Start processing
            logger.info("Starting payment processor...");
            processor.start();
            
        } catch (Exception e) {
            logger.error("Failed to start payment processor", e);
            System.exit(1);
        }
    }
} 