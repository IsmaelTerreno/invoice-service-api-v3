package com.remotejob.planservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "PlanPatch", description = "Fields to partially update in a plan. Only non-null fields will be applied.")
public class PlanPatchDto {
    @Schema(description = "New description for the plan", example = "Basic subscription plan - patched", nullable = true)
    public String description;

    @Schema(description = "Whether the plan is active", example = "false", nullable = true)
    public Boolean isActive;

    @Schema(description = "Partial or full items payload to replace existing items",
            implementation = JsonNode.class,
            example = "{\n  \"planName\": \"basic\",\n  \"seats\": 2,\n  \"features\": [\"support\", \"export\"]\n}",
            nullable = true)
    public JsonNode items;

    @Schema(description = "New status for this plan", example = "PAID", nullable = true)
    public String status;

    @Schema(description = "New duration in days", example = "60", nullable = true)
    public Integer durationInDays;

    @Schema(description = "New expiration time", example = "2026-01-31T00:00:00Z", nullable = true)
    public Instant expiresAt;
}
