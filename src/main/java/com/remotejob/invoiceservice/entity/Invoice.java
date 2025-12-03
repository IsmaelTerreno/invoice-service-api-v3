package com.remotejob.invoiceservice.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an Invoice entity.
 *
 * @property id - The unique identifier for the invoice.
 * @property userId - The identifier for the user associated with the invoice.
 * @property customerId - The identifier for the customer associated with the invoice.
 * @property customerEmail - The email of the customer associated with the invoice.
 * @property items - The items included in the invoice.
 * @property subscriptionId - The identifier for the subscription associated with the invoice.
 * @property status - The status of the invoice.
 * @property lastPaymentIntentId - The identifier for the last payment intent associated with the invoice.
 * @property invoiceIdProvidedByStripe - The identifier for the invoice as provided by Stripe.
 * @property createdAt - The timestamp when the invoice was created.
 * @property updatedAt - The timestamp when the invoice was last updated.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "invoice")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Convert(converter = JsonDynamicConverter.class)
    @Column(name = "items", nullable = false, length = 10000)
    private JsonNode items;

    @Column(name = "subscription_id", nullable = true)
    private String subscriptionId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "last_payment_intent_id", nullable = false)
    private String lastPaymentIntentId;

    @Column(name = "invoice_id_provided_by_stripe", nullable = false)
    private String invoiceIdProvidedByStripe;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
