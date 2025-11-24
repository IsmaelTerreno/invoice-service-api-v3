package com.remotejob.invoiceservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an Invoice Data Transfer Object.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Invoice DTO")
public class InvoiceDto {
    @Schema(description = "This will identify the Invoice in our database.")
    private UUID id;

    @Schema(description = "User ID of the Invoice.")
    @NotNull(message = "User ID is required")
    private String userId;

    @Schema(description = "Customer ID of the Invoice.")
    @NotNull(message = "Customer ID is required")
    private String customerId;

    @Schema(description = "Customer Email of the Invoice.")
    @NotNull(message = "Customer Email is required")
    @Email(message = "Customer Email must be valid")
    private String customerEmail;

    @Schema(description = "Items of the Invoice.")
    private JsonNode items;

    @Schema(description = "Subscription ID of the Invoice.")
    @NotNull(message = "Subscription ID is required")
    private String subscriptionId;

    @Schema(description = "Status of the Invoice.")
    @NotNull(message = "Status is required")
    private String status;

    @Schema(description = "Last Payment Intent ID of the Invoice.")
    private String lastPaymentIntentId;

    @Schema(description = "Invoice ID provided by Stripe.")
    private String invoiceIdProvidedByStripe;

    @Schema(description = "Creation timestamp.")
    private Instant createdAt;

    @Schema(description = "Last update timestamp.")
    private Instant updatedAt;
}
