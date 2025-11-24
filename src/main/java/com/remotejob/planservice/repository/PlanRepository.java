package com.remotejob.planservice.repository;

import com.remotejob.planservice.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Plan entities.
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    /**
     * Retrieves a list of plans associated with a specific user ID.
     */
    List<Plan> findByUserId(String id);

    /**
     * Retrieves a plan by userId and invoiceId.
     */
    Optional<Plan> findByUserIdAndInvoiceId(String userId, UUID invoiceId);
}
