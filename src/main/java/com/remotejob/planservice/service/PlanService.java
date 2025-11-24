package com.remotejob.planservice.service;

import com.remotejob.planservice.dto.PlanPatchDto;
import com.remotejob.planservice.entity.Plan;
import com.remotejob.planservice.repository.PlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing job-related operations. This class interacts with the
 * PlanRepository to perform CRUD operations and provides methods to retrieve jobs based
 * on various criteria.
 */
@Service
public class PlanService {
    private final PlanRepository planRepository;


    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    /**
     * Retrieves all job entities from the repository.
     *
     * @return A list of all jobs available in the repository.
     */
    public List<Plan> findAll() {
        return this.planRepository.findAll();
    }

    /**
     * Creates a new job or updates an existing one in the repository.
     *
     * @param plan The Job object to be created or updated.
     * @return The saved Job object.
     */
    public Plan createOrUpdate(Plan plan) {
        return this.planRepository.save(plan);
    }

    /**
     * Deletes a job by its unique identifier.
     *
     * @param id The UUID of the job to delete.
     */
    public void delete(UUID id) {
        this.planRepository.deleteById(id);
    }

    /**
     * Retrieves a job by its unique identifier.
     *
     * @param id The UUID of the job to retrieve.
     * @return An Optional containing the Job if found, or an empty Optional if not found.
     */
    public Optional<Plan> getById(UUID id) {
        return this.planRepository.findById(id);
    }

    /**
     * Retrieves a list of jobs associated with a specific user ID.
     *
     * @param userId The ID of the user whose jobs are to be retrieved.
     * @return A list of jobs associated with the specified user ID.
     */
    public List<Plan> getByUserId(String userId) {
        return this.planRepository.findByUserId(userId);
    }

    /**
     * Retrieves a plan by userId and invoiceId.
     */
    public Optional<Plan> getByUserIdAndInvoiceId(String userId, UUID invoiceId) {
        return this.planRepository.findByUserIdAndInvoiceId(userId, invoiceId);
    }

    /**
     * Partially updates a plan with the non-null fields provided in the patch DTO.
     *
     * @param id    the plan id to update
     * @param patch the fields to apply (only non-null values will be updated)
     * @return the updated plan, or empty if not found
     */
    public Optional<Plan> partialUpdate(UUID id, PlanPatchDto patch) {
        Optional<Plan> existingOpt = planRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }
        Plan plan = existingOpt.get();
        if (patch.description != null) plan.setDescription(patch.description);
        if (patch.isActive != null) plan.setIsActive(patch.isActive);
        if (patch.items != null) plan.setItems(patch.items);
        if (patch.status != null) plan.setStatus(patch.status);
        if (patch.durationInDays != null) plan.setDurationInDays(patch.durationInDays);
        if (patch.expiresAt != null) plan.setExpiresAt(patch.expiresAt);
        Plan saved = planRepository.save(plan);
        return Optional.of(saved);
    }
}
