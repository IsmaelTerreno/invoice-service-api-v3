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
 * Represents a subscription for a user.
 *
 * Properties:
 *  - userId: User ID for the subscription.
 *  - email: Customer Email of the subscription.
 *  - items: Items of the subscription.
 *  - paymentMethod: Payment Method of the subscription.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Subscription DTO for creating a new subscription")
public class SubscriptionDto {
    @Schema(description = "User ID for the subscription.", example = "user-123")
    @NotNull(message = "User ID is required")
    private String userId;

    @Schema(description = "Customer Email of the subscription.", example = "user@example.com")
    @NotNull(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(description = "Items of the subscription.")
    @NotNull(message = "Items are required")
    private JsonNode items;

    @Schema(description = "Payment Method of the subscription.", example = "pm_card_visa")
    @JsonProperty("payment_method")
    private String paymentMethod;
}
