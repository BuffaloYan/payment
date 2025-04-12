-- Create the payment_requests table
CREATE TABLE IF NOT EXISTS payment_requests (
    request_id VARCHAR(36) PRIMARY KEY,
    payer_account_number VARCHAR(50) NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_payment_requests_payer_account ON payment_requests(payer_account_number);
CREATE INDEX IF NOT EXISTS idx_payment_requests_timestamp ON payment_requests(timestamp);

-- Create a view for recent payments
CREATE OR REPLACE VIEW recent_payments AS
SELECT 
    request_id,
    payer_account_number,
    payment_type,
    amount,
    timestamp,
    created_at
FROM payment_requests
ORDER BY timestamp DESC
LIMIT 1000; 