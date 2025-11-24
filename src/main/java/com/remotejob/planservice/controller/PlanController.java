package com.remotejob.planservice.controller;

import com.remotejob.planservice.dto.PlanDto;
import com.remotejob.planservice.dto.ResponseAPI;
import com.remotejob.planservice.dto.PlanPatchDto;
import com.remotejob.planservice.entity.Plan;
import com.remotejob.planservice.mapper.PlanMapper;
import com.remotejob.planservice.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Rest controller to manage job-related operations.
 */
@Slf4j
@RestController
@Tag(name = "Plan")
@RequestMapping("/api/v1/plan")
public class PlanController {
    /**
     * Represents a service responsible for managing job-related operations.
     * This service handles the creation, update, retrieval, and deletion
     * of plan entities within the system.
     */
    private final PlanService planService;
    private final PlanMapper planMapper;

    public PlanController(PlanService planService, PlanMapper planMapper) {
        this.planService = planService;
        this.planMapper = planMapper;
    }

    /**
     * Retrieves a plan by its unique identifier.
     *
     * @param id The UUID of the plan to retrieve.
     * @return A ResponseAPI object containing an Optional with the Job if found,
     * or an empty Optional if not found.
     */
    @Operation(summary = "Get plan by ID")
    @ApiResponse(responseCode = "200", description = "Plan found",
            content = @Content(schema = @Schema(implementation = PlanDto.class)))
    @GetMapping("/{id}")
    public ResponseAPI<Optional<PlanDto>> getById(@PathVariable(value = "id") UUID id) {
        Optional<Plan> plan = this.planService.getById(id);
        return new ResponseAPI<>("Success", plan.map(planMapper::toDto));
    }

    /**
     * Retrieves a list of plans associated with a specific user ID.
     *
     * @param userId The ID of the user whose plans are to be retrieved.
     * @return A ResponseAPI object containing a list of plans associated with the specified user ID.
     */
    @Operation(summary = "Get plans by user ID")
    @ApiResponse(responseCode = "200", description = "Plans for user",
            content = @Content(schema = @Schema(implementation = PlanDto.class)))
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/{userId}")
    public ResponseAPI<List<PlanDto>> getByUserId(@PathVariable(value = "userId") String userId) {
        List<PlanDto> result = this.planService.getByUserId(userId).stream()
                .map(planMapper::toDto)
                .toList();
        return new ResponseAPI<>("Success", result);
    }

    /**
     * Creates a new plan or updates an existing one.
     *
     * @param planDto The plan to be created or updated.
     * @return A ResponseAPI object containing the created or updated plan.
     */
    @Operation(summary = "Create a plan")
    @ApiResponse(responseCode = "200", description = "Plan created",
            content = @Content(schema = @Schema(implementation = PlanDto.class)))
    @PreAuthorize("hasRole('USER')")
    @PostMapping()
    public ResponseAPI<PlanDto> create(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PlanDto.class),
                            examples = @ExampleObject(
                                    name = "CreatePlanExample",
                                    summary = "Quick create example",
                                    value = "{\n  \"userId\": \"user-12345\",\n  \"invoiceId\": \"f54a0b7c-12d3-4e5f-a6b7-8c9d0e1f2a3b\",\n  \"description\": \"Basic subscription plan\",\n  \"isActive\": true,\n  \"items\": {\n    \"planName\": \"basic\",\n    \"seats\": 1,\n    \"features\": [\"support\"]\n  },\n  \"status\": \"CREATED\",\n  \"durationInDays\": 30,\n  \"expiresAt\": \"2025-12-31T23:59:59Z\"\n}"
                            )
                    )
            ) @org.springframework.web.bind.annotation.RequestBody PlanDto planDto) {
        Plan saved = planService.createOrUpdate(planMapper.fromDto(planDto));
        return new ResponseAPI<>("Success", planMapper.toDto(saved));
    }

    /**
     * Updates an existing plan.
     *
     * @param planDto The plan to be updated.
     * @return A ResponseAPI object containing the updated plan.
     */
    @Operation(summary = "Update a plan")
    @ApiResponse(responseCode = "200", description = "Plan updated",
            content = @Content(schema = @Schema(implementation = PlanDto.class)))
    @PreAuthorize("hasRole('USER')")
    @PutMapping()
    public ResponseAPI<PlanDto> update(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PlanDto.class),
                            examples = @ExampleObject(
                                    name = "UpdatePlanExample",
                                    summary = "Quick update example",
                                    value = "{\n  \"id\": \"8b0a1d1a-1a2b-4c3d-8e9f-1234567890ab\",\n  \"userId\": \"user-12345\",\n  \"invoiceId\": \"f54a0b7c-12d3-4e5f-a6b7-8c9d0e1f2a3b\",\n  \"description\": \"Basic subscription plan - updated\",\n  \"isActive\": false,\n  \"items\": {\n    \"planName\": \"basic\",\n    \"seats\": 2,\n    \"features\": [\"support\", \"export\"]\n  },\n  \"status\": \"PAID\",\n  \"durationInDays\": 60,\n  \"expiresAt\": \"2026-01-31T00:00:00Z\"\n}"
                            )
                    )
            ) @org.springframework.web.bind.annotation.RequestBody PlanDto planDto) {
        Plan saved = planService.createOrUpdate(planMapper.fromDto(planDto));
        return new ResponseAPI<>("Success", planMapper.toDto(saved));
    }

    /**
     * Deletes a plan by its unique identifier.
     *
     * @param id The UUID of the plan to delete.
     * @return A ResponseAPI object containing a success message and no data.
     */
    @Operation(summary = "Delete a plan")
    @ApiResponse(responseCode = "200", description = "Plan deleted")
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{id}")
    public ResponseAPI<PlanDto> delete(@PathVariable(value = "id") UUID id) {
        planService.delete(id);
        return new ResponseAPI<>("Plan deleted successfully", null);
    }

    /**
     * Partially updates fields of an existing plan. Only non-null fields in the payload are applied.
     *
     * @param id   The UUID of the plan to update.
     * @param patch The fields to update.
     * @return A ResponseAPI containing the updated plan, or null if not found.
     */
    @Operation(summary = "Partially update a plan")
    @ApiResponse(responseCode = "200", description = "Plan patched",
            content = @Content(schema = @Schema(implementation = PlanDto.class)))
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{id}")
    public ResponseAPI<PlanDto> patch(
            @PathVariable("id") UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PlanPatchDto.class),
                            examples = @ExampleObject(
                                    name = "PatchPlanExample",
                                    summary = "Quick patch example",
                                    value = "{\n  \"description\": \"Patched description\",\n  \"isActive\": false,\n  \"status\": \"PAID\"\n}"
                            )
                    )
            ) @org.springframework.web.bind.annotation.RequestBody PlanPatchDto patch
    ) {
        Optional<Plan> updated = planService.partialUpdate(id, patch);
        return new ResponseAPI<>("Success", updated.map(planMapper::toDto).orElse(null));
    }
}
