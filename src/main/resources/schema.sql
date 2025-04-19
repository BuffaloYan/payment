CREATE TABLE IF NOT EXISTS payment_requests (
    request_id VARCHAR(36) PRIMARY KEY,
    payer_account_number VARCHAR(50) NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reply_topic VARCHAR(255) NOT NULL
); 