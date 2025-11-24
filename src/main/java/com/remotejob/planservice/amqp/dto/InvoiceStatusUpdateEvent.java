package com.remotejob.planservice.amqp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "InvoiceStatusUpdateEvent", description = "Event payload to update plan status based on invoice changes")
public class InvoiceStatusUpdateEvent {
    @Schema(description = "User ID related to the plan", required = true, example = "user-12345")
    public String userId;

    @Schema(description = "Invoice ID related to the plan", required = true, example = "f54a0b7c-12d3-4e5f-a6b7-8c9d0e1f2a3b")
    public UUID invoiceId;

    @Schema(description = "New status for the related plan", required = true, example = "PAID")
    public String status;

    @Schema(description = "Whether the plan is active after the update", example = "true")
    public Boolean isActive;

    @Schema(description = "Optional new expiration time for the plan", example = "2026-01-31T00:00:00Z")
    public Instant expiresAt;
}
