package com.remotejob.invoiceservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a Notification Data Transfer Object.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Notification DTO")
public class NotificationDto {
    @Schema(description = "Unique ID in the DB for the notification.")
    private String id;

    @Schema(description = "User ID related to this notification")
    @NotNull(message = "User ID is required")
    private String userId;

    @Schema(description = "User email to send the notification to")
    @NotNull(message = "User email is required")
    @Email(message = "User email must be valid")
    private String userEmail;

    @Schema(description = "User full name for personalized notifications")
    @NotNull(message = "Full name is required")
    private String fullName;

    @Schema(description = "Event type related to this notification")
    @NotNull(message = "Event type is required")
    private String eventType;

    @Schema(description = "Type of notification (payment, account, system, marketing)")
    private String notificationType;

    @Schema(description = "Topic/Subject of the notification")
    @NotNull(message = "Topic is required")
    private String topic;

    @Schema(description = "Body/Detail of the notification")
    @NotNull(message = "Body is required")
    private String body;

    @Schema(description = "Read status of the notification")
    @NotNull(message = "Read status is required")
    private Boolean read;

    @Schema(description = "Creation date")
    private Instant createdAt;

    @Schema(description = "Update date")
    private Instant updatedAt;
}
