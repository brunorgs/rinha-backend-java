CREATE TABLE payment (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    fallback BOOLEAN NOT NULL DEFAULT FALSE,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
);

-- Índices para melhorar performance
CREATE INDEX idx_payment_fallback ON payment(fallback);
CREATE INDEX idx_payment_requested_at ON payment(requested_at);