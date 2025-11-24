package com.remotejob.planservice.amqp.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "PlansToCreateEvent", description = "Event payload to create or update plans from async messages")
public class PlansToCreateEvent {
    @Schema(description = "Existing plan ID for updates (omit for create)", example = "8b0a1d1a-1a2b-4c3d-8e9f-1234567890ab")
    public UUID id; // optional for updates

    @Schema(description = "User ID", required = true, example = "user-12345")
    public String userId;

    @Schema(description = "Invoice ID", required = true, example = "f54a0b7c-12d3-4e5f-a6b7-8c9d0e1f2a3b")
    public UUID invoiceId;

    @Schema(description = "Description", example = "Basic subscription plan")
    public String description;

    @Schema(description = "Active flag", example = "true")
    public Boolean isActive;

    @Schema(description = "Items payload",
            implementation = JsonNode.class,
            example = "{\n  \"planName\": \"basic\",\n  \"seats\": 1,\n  \"features\": [\"support\"]\n}")
    public JsonNode items;

    @Schema(description = "Status", example = "CREATED")
    public String status;

    @Schema(description = "Duration in days", example = "30")
    public Integer durationInDays;

    @Schema(description = "Expiration timestamp (ISO-8601)", example = "2025-12-31T23:59:59Z")
    public Instant expiresAt;
}
