-- V3: Add customer_full_name column to invoice table
-- Adds support for storing customer's full name in invoices

-- Add customer_full_name column to store customer's full name
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS customer_full_name VARCHAR(100);

-- Create index for customer_full_name for efficient searches
CREATE INDEX IF NOT EXISTS idx_invoice_customer_full_name ON invoice(customer_full_name);

-- Add comment to the new column
COMMENT ON COLUMN invoice.customer_full_name IS 'Full name of the customer as provided during payment/subscription';

