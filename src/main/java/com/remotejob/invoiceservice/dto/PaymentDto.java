package com.remotejob.invoiceservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a one-time payment for a user.
 *
 * Properties:
 *  - userId: User ID for the payment.
 *  - email: Customer Email of the payment.
 *  - items: Items to be paid for.
 *  - paymentMethod: Payment Method for the payment.
 *  - currency: Currency for the payment (default: usd).
 *  - jobId: Job ID associated with the payment (optional).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payment DTO for creating a one-time payment")
public class PaymentDto {
    @Schema(description = "User ID for the payment.", example = "user-123")
    @NotNull(message = "User ID is required")
    private String userId;

    @Schema(description = "Customer Email of the payment.", example = "user@example.com")
    @NotNull(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(description = "Customer full name.", example = "John Doe", required = true)
    @NotNull(message = "Full name is required")
    @JsonProperty("full_name")
    private String fullName;

    @Schema(description = "Items to be paid for.")
    @NotNull(message = "Items are required")
    private JsonNode items;

    @Schema(description = "Payment Method for the payment.", example = "pm_card_visa")
    @NotNull(message = "Payment method is required")
    @JsonProperty("payment_method")
    private String paymentMethod;

    @Schema(description = "Currency for the payment.", example = "usd", defaultValue = "usd")
    private String currency = "usd";

    @Schema(description = "Job ID associated with the payment.", example = "job-456")
    @JsonProperty("job_id")
    private String jobId;

    @Schema(description = "Plan metadata (features selected)",
            example = "{\"showLogo\": true, \"brandColor\": \"#FF6B6B\", \"highlightYellow\": true, \"showOnTop\": true}")
    private JsonNode metadata;
}

