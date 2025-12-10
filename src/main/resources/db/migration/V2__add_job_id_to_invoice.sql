-- V2: Add job_id column to invoice table
-- Adds support for tracking jobs associated with invoices

-- Add job_id column to invoice table
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS job_id VARCHAR(255);

-- Create index for job_id for efficient lookups
CREATE INDEX IF NOT EXISTS idx_invoice_job_id ON invoice(job_id);

-- Add comment to new column
COMMENT ON COLUMN invoice.job_id IS 'Job identifier associated with the invoice (optional)';

