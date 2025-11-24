package com.remotejob.planservice.service;

import com.remotejob.planservice.amqp.dto.InvoiceStatusUpdateEvent;
import com.remotejob.planservice.amqp.dto.PlansToCreateEvent;
import com.remotejob.planservice.entity.Plan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceWorkerService {

    private final PlanService planService;

    public void handleInvoiceStatusUpdate(InvoiceStatusUpdateEvent event) {
        Instant start = Instant.now();
        putCorrelation(event.userId, event.invoiceId);
        try {
            log.info("[AMQP] InvoiceStatusUpdateEvent received");
            log.debug("Payload -> userId={}, invoiceId={}, status={}, isActive={}, expiresAt={}",
                    event.userId, event.invoiceId, event.status, event.isActive, event.expiresAt);

            if (event.userId == null || event.invoiceId == null) {
                log.error("Missing required identifiers in event: userId={}, invoiceId={}", event.userId, event.invoiceId);
                return;
            }

            Optional<Plan> existing = planService.getByUserIdAndInvoiceId(event.userId, event.invoiceId);
            if (existing.isEmpty()) {
                log.warn("No plan found for userId={} and invoiceId={}", event.userId, event.invoiceId);
                return;
            }

            Plan plan = existing.get();
            String before = safePlanSnapshot(plan);

            if (event.status != null) plan.setStatus(event.status);
            if (event.isActive != null) plan.setIsActive(event.isActive);
            if (event.expiresAt != null) plan.setExpiresAt(event.expiresAt);

            Plan saved = planService.createOrUpdate(plan);

            String after = safePlanSnapshot(saved);
            log.info("Plan updated successfully (id={})", saved.getId());
            log.debug("Before: {}", before);
            log.debug("After : {}", after);
        } catch (Exception ex) {
            log.error("Error while handling InvoiceStatusUpdateEvent", ex);
        } finally {
            log.info("InvoiceStatusUpdateEvent processed in {} ms", Duration.between(start, Instant.now()).toMillis());
            clearCorrelation();
        }
    }

    public void handlePlansToCreate(PlansToCreateEvent event) {
        Instant start = Instant.now();
        putCorrelation(event.userId, event.invoiceId);
        try {
            log.info("[AMQP] PlansToCreateEvent received");
            log.debug("Payload -> id={}, userId={}, invoiceId={}, desc={}, isActive={}, status={}, duration={}, expiresAt={}",
                    event.id, event.userId, event.invoiceId, event.description, event.isActive, event.status, event.durationInDays, event.expiresAt);

            if (event.userId == null || event.invoiceId == null) {
                log.error("Missing required identifiers in event: userId={}, invoiceId={}", event.userId, event.invoiceId);
                return;
            }

            Plan plan = new Plan();
            plan.setId(event.id);
            plan.setUserId(event.userId);
            plan.setInvoiceId(event.invoiceId);
            plan.setDescription(event.description);
            plan.setIsActive(event.isActive);
            plan.setItems(event.items);
            plan.setStatus(event.status);
            plan.setDurationInDays(event.durationInDays);
            plan.setExpiresAt(event.expiresAt);

            String before = "<new>";
            Plan saved = planService.createOrUpdate(plan);
            String after = safePlanSnapshot(saved);

            log.info("Plan saved/updated successfully (id={})", saved.getId());
            log.debug("Before: {}", before);
            log.debug("After : {}", after);
        } catch (Exception ex) {
            log.error("Error while handling PlansToCreateEvent", ex);
        } finally {
            log.info("PlansToCreateEvent processed in {} ms", Duration.between(start, Instant.now()).toMillis());
            clearCorrelation();
        }
    }

    private void putCorrelation(String userId, java.util.UUID invoiceId) {
        if (userId != null) {
            MDC.put("userId", userId);
        }
        if (invoiceId != null) {
            MDC.put("invoiceId", String.valueOf(invoiceId));
        }
    }

    private void clearCorrelation() {
        MDC.remove("userId");
        MDC.remove("invoiceId");
    }

    private String safePlanSnapshot(Plan p) {
        if (p == null) return "<null>";
        return String.format("Plan{id=%s, userId=%s, invoiceId=%s, status=%s, isActive=%s, durationInDays=%s, expiresAt=%s}",
                p.getId(), p.getUserId(), p.getInvoiceId(), p.getStatus(), p.getIsActive(), p.getDurationInDays(), p.getExpiresAt());
    }
}
