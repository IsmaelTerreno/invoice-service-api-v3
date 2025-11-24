package com.remotejob.planservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "Plan", description = "Subscription plan DTO")
public class PlanDto {
    @Schema(description = "Unique ID in the DB for the plan",
            example = "8b0a1d1a-1a2b-4c3d-8e9f-1234567890ab",
            nullable = true)
    public UUID id;

    @NotBlank
    @Schema(description = "User ID related to this plan", example = "user-12345")
    public String userId;

    @NotNull
    @Schema(description = "Invoice ID related to this plan", example = "f54a0b7c-12d3-4e5f-a6b7-8c9d0e1f2a3b")
    public UUID invoiceId;

    @NotBlank
    @Schema(description = "General description of this plan", example = "Basic subscription plan")
    public String description;

    @NotNull
    @Schema(description = "If this plan is active or not", example = "true")
    public Boolean isActive;

    @NotNull
    @Schema(description = "The items payload for the plan",
            implementation = JsonNode.class,
            example = "{\n  \"planName\": \"basic\",\n  \"seats\": 1,\n  \"features\": [\"support\"]\n}")
    public JsonNode items;

    @NotBlank
    @Schema(description = "Current status of this plan", example = "CREATED")
    public String status;

    @NotNull
    @Schema(description = "Duration in days of this plan", example = "30")
    public Integer durationInDays;

    @Schema(description = "Expiration date of the plan",
            nullable = true,
            example = "2025-12-31T23:59:59Z")
    public Instant expiresAt;
}
