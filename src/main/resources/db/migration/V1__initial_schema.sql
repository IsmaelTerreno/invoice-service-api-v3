-- V1: Initial schema for invoice-service-api-v3
-- Creates the invoice table with all necessary columns and indexes

-- Create invoice table
CREATE TABLE IF NOT EXISTS invoice (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    subscription_id VARCHAR(255),  -- Nullable to support one-time payments
    invoice_id_provided_by_stripe VARCHAR(255) NOT NULL,
    last_payment_intent_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    items TEXT NOT NULL,  -- Stores JSON data (using TEXT for better compatibility)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_invoice_user_id ON invoice(user_id);
CREATE INDEX IF NOT EXISTS idx_invoice_customer_id ON invoice(customer_id);
CREATE INDEX IF NOT EXISTS idx_invoice_customer_email ON invoice(customer_email);
CREATE INDEX IF NOT EXISTS idx_invoice_subscription_id ON invoice(subscription_id);
CREATE INDEX IF NOT EXISTS idx_invoice_status ON invoice(status);
CREATE INDEX IF NOT EXISTS idx_invoice_created_at ON invoice(created_at DESC);

-- Add comment to table
COMMENT ON TABLE invoice IS 'Stores invoice records for both subscriptions and one-time payments';
COMMENT ON COLUMN invoice.id IS 'Primary key (UUID)';
COMMENT ON COLUMN invoice.user_id IS 'User identifier from the authentication service';
COMMENT ON COLUMN invoice.customer_id IS 'Stripe customer ID';
COMMENT ON COLUMN invoice.customer_email IS 'Customer email address';
COMMENT ON COLUMN invoice.subscription_id IS 'Stripe subscription ID (null for one-time payments)';
COMMENT ON COLUMN invoice.invoice_id_provided_by_stripe IS 'Stripe invoice or payment intent ID';
COMMENT ON COLUMN invoice.last_payment_intent_id IS 'Last Stripe payment intent ID';
COMMENT ON COLUMN invoice.status IS 'Payment status (e.g., succeeded, requires_payment_method, etc.)';
COMMENT ON COLUMN invoice.items IS 'JSON array of purchased items/plans';
COMMENT ON COLUMN invoice.created_at IS 'Timestamp when the invoice was created';
COMMENT ON COLUMN invoice.updated_at IS 'Timestamp when the invoice was last updated';

