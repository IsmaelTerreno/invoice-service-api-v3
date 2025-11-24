package com.remotejob.invoiceservice.repository;

import com.remotejob.invoiceservice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Invoice entity operations.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    /**
     * Finds an invoice by customer ID and invoice ID provided by Stripe.
     *
     * @param customerId The customer ID from Stripe
     * @param invoiceIdProvidedByStripe The invoice ID provided by Stripe
     * @return An Optional containing the invoice if found
     */
    Optional<Invoice> findByCustomerIdAndInvoiceIdProvidedByStripe(String customerId, String invoiceIdProvidedByStripe);
}
