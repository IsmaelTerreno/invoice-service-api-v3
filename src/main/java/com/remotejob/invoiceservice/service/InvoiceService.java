package com.remotejob.invoiceservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.remotejob.invoiceservice.amqp.EventPatternNotification;
import com.remotejob.invoiceservice.dto.NotificationDto;
import com.remotejob.invoiceservice.dto.PaymentDto;
import com.remotejob.invoiceservice.dto.SubscriptionDto;
import com.remotejob.invoiceservice.entity.Invoice;
import com.remotejob.invoiceservice.repository.InvoiceRepository;
import com.remotejob.invoiceservice.util.CorrelationContext;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for handling invoice operations and Stripe webhook events.
 */
@Service
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StripeService stripeService;
    private final RabbitMQService rabbitMQService;

    @Value("${rabbitmq.queue.invoice-status-on-related-plans}")
    private String invoiceStatusOnRelatedPlansQueueName;

    @Value("${rabbitmq.queue.plans-to-create}")
    private String plansToCreateQueueName;

    @Value("${rabbitmq.queue.notification-events}")
    private String notificationEventsQueueName;

    private final Map<String, Consumer<Event>> eventHandlers = new HashMap<>();

    public InvoiceService(InvoiceRepository invoiceRepository, StripeService stripeService, RabbitMQService rabbitMQService) {
        this.invoiceRepository = invoiceRepository;
        this.stripeService = stripeService;
        this.rabbitMQService = rabbitMQService;

        // Initialize event handlers
        eventHandlers.put("invoice.created", this::handleInvoiceCreated);
        eventHandlers.put("invoice.updated", this::handleInvoiceUpdated);
        eventHandlers.put("payment_intent.succeeded", this::handlePaymentIntentSucceeded);
    }

    /**
     * Retrieves all invoices from the repository.
     *
     * @return A list of all Invoice objects.
     */
    public List<Invoice> findAll() {
        log.info("Retrieving all invoices");
        return invoiceRepository.findAll();
    }

    /**
     * Saves a new invoice or updates an existing one.
     *
     * @param invoiceToCreate The invoice object to be created or updated.
     * @return The saved or updated invoice.
     */
    public Invoice saveOrUpdate(Invoice invoiceToCreate) {
        log.info("Saving or updating invoice for user: {}", invoiceToCreate.getUserId());
        return invoiceRepository.save(invoiceToCreate);
    }

    /**
     * Creates a new subscription and saves the corresponding invoice.
     *
     * @param subscriptionToCreate The data required to create the subscription.
     * @return A map containing the status, message, and saved invoice data.
     * @throws StripeException if any Stripe operation fails.
     */
    public Map<String, Object> createSubscriptionAndSaveInvoice(SubscriptionDto subscriptionToCreate) throws StripeException {
        Instant startTime = Instant.now();
        
        // Set tracking context
        CorrelationContext.setUserId(subscriptionToCreate.getUserId());
        
        log.info("🔄 [SUBSCRIPTION] Starting subscription creation | userId={} | email={} | jobId={}", 
                subscriptionToCreate.getUserId(), subscriptionToCreate.getEmail(),
                subscriptionToCreate.getJobId() != null ? subscriptionToCreate.getJobId() : "NOT_PROVIDED");
        
        if (subscriptionToCreate.getJobId() != null) {
            log.info("📌 [SUBSCRIPTION-JOB] Job ID detected in subscription request | jobId={} | userId={}", 
                    subscriptionToCreate.getJobId(), subscriptionToCreate.getUserId());
        } else {
            log.warn("⚠️ [SUBSCRIPTION-JOB] No job ID provided in subscription request | userId={} | This subscription will not be linked to a specific job", 
                    subscriptionToCreate.getUserId());
        }

        // Create new customer in Stripe
        com.stripe.model.Customer customer = stripeService.createCustomer(
                subscriptionToCreate.getEmail(),
                subscriptionToCreate.getPaymentMethod()
        );
        CorrelationContext.setCustomerId(customer.getId());
        log.info("✅ [SUBSCRIPTION] Created Stripe customer | customerId={} | email={}", 
                customer.getId(), customer.getEmail());

        // Create new subscription in Stripe
        Subscription subscription = stripeService.createSubscription(
                customer.getId(),
                subscriptionToCreate.getItems(),
                subscriptionToCreate.getPaymentMethod()
        );
        CorrelationContext.setSubscriptionId(subscription.getId());
        log.info("✅ [SUBSCRIPTION] Created Stripe subscription | subscriptionId={} | status={}", 
                subscription.getId(), subscription.getStatus());

        // Get the latest invoice and payment intent
        com.stripe.model.Invoice latestInvoice = (com.stripe.model.Invoice) subscription.getLatestInvoiceObject();
        PaymentIntent paymentIntent = (PaymentIntent) latestInvoice.getPaymentIntentObject();
        CorrelationContext.setPaymentIntentId(paymentIntent.getId());
        
        log.debug("[SUBSCRIPTION] Retrieved payment details | stripeInvoiceId={} | paymentIntentId={} | amount={}", 
                latestInvoice.getId(), paymentIntent.getId(), paymentIntent.getAmount());

        // Prepare invoice to create
        Invoice invoiceToCreate = new Invoice();
        invoiceToCreate.setUserId(subscriptionToCreate.getUserId());
        invoiceToCreate.setCustomerId(customer.getId());
        invoiceToCreate.setCustomerEmail(customer.getEmail());
        invoiceToCreate.setItems(subscriptionToCreate.getItems());
        invoiceToCreate.setSubscriptionId(subscription.getId());
        invoiceToCreate.setStatus(latestInvoice.getStatus());
        invoiceToCreate.setLastPaymentIntentId(paymentIntent.getId());
        invoiceToCreate.setInvoiceIdProvidedByStripe(latestInvoice.getId());
        invoiceToCreate.setJobId(subscriptionToCreate.getJobId()); // Set job ID if provided

        // Create new invoice in DB from Stripe subscription information
        Invoice savedInvoice = saveOrUpdate(invoiceToCreate);
        CorrelationContext.setInvoiceId(savedInvoice.getId().toString());
        
        log.info("✅ [SUBSCRIPTION] Saved invoice to DB | invoiceId={} | userId={} | customerId={} | status={} | jobId={}", 
                savedInvoice.getId(), savedInvoice.getUserId(), savedInvoice.getCustomerId(), 
                savedInvoice.getStatus(), savedInvoice.getJobId() != null ? savedInvoice.getJobId() : "NULL");
        
        if (savedInvoice.getJobId() != null) {
            log.info("📌 [SUBSCRIPTION-JOB] Invoice linked to job | invoiceId={} | jobId={}", 
                    savedInvoice.getId(), savedInvoice.getJobId());
        }

        // Confirm payment intent to complete the subscription
        log.info("🔄 [SUBSCRIPTION] Confirming payment intent | paymentIntentId={}", paymentIntent.getId());
        PaymentIntent confirmedPaymentIntent = stripeService.confirmPaymentIntent(
                paymentIntent.getId(),
                subscriptionToCreate.getPaymentMethod()
        );
        log.info("✅ [SUBSCRIPTION] Payment intent confirmed | paymentIntentId={} | status={}", 
                confirmedPaymentIntent.getId(), confirmedPaymentIntent.getStatus());

        log.info("🛰 [SUBSCRIPTION->PLAN] Initiating plan creation | userId={} | invoiceId={} | jobId={} | subscriptionId={}", 
                subscriptionToCreate.getUserId(), savedInvoice.getId(), 
                savedInvoice.getJobId() != null ? savedInvoice.getJobId() : "NULL", subscription.getId());

        // Send message to RabbitMQ service to create a plan for the related invoice
        Map<String, Object> planData = new HashMap<>();
        planData.put("userId", subscriptionToCreate.getUserId());
        planData.put("invoiceId", savedInvoice.getId());
        planData.put("description", extractDescription(savedInvoice.getItems()));
        planData.put("items", savedInvoice.getItems());
        planData.put("isActive", false);
        planData.put("status", subscription.getStatus());
        planData.put("durationInDays", 30);
        if (savedInvoice.getJobId() != null) {
            planData.put("jobId", savedInvoice.getJobId());
            log.info("📌 [SUBSCRIPTION->PLAN-JOB] Including job ID in RabbitMQ message | jobId={} | invoiceId={}", 
                    savedInvoice.getJobId(), savedInvoice.getId());
        } else {
            log.warn("⚠️ [SUBSCRIPTION->PLAN-JOB] No job ID to include in RabbitMQ message | invoiceId={}", 
                    savedInvoice.getId());
        }
        if (subscriptionToCreate.getMetadata() != null) {
            planData.put("metadata", subscriptionToCreate.getMetadata());
            log.info("📦 [SUBSCRIPTION->PLAN] Including metadata in RabbitMQ message | invoiceId={} | metadata={}", 
                    savedInvoice.getId(), subscriptionToCreate.getMetadata().toString());
        }

        rabbitMQService.sendDirectMessage(planData, plansToCreateQueueName);

        log.info("✅ [SUBSCRIPTION->PLAN] Plan creation message sent | userId={} | invoiceId={} | jobId={} | isActive=false", 
                subscriptionToCreate.getUserId(), savedInvoice.getId(), 
                savedInvoice.getJobId() != null ? savedInvoice.getJobId() : "NULL");

        // Prepare notification message for the user
        NotificationDto notificationMessage = new NotificationDto();
        notificationMessage.setUserId(savedInvoice.getUserId());
        notificationMessage.setUserEmail(savedInvoice.getCustomerEmail());
        notificationMessage.setNotificationType("payment");
        notificationMessage.setTopic("Payment in progress");
        notificationMessage.setBody("Waiting for payment");
        notificationMessage.setRead(false);
        notificationMessage.setEventType(EventPatternNotification.PAYMENT_IN_PROGRESS_NOTIFICATION.getPattern());
        notificationMessage.setCreatedAt(Instant.now());

        // Send message to RabbitMQ service to notify the user about the invoice status update
        rabbitMQService.createMessageMQService(
                notificationMessage,
                notificationEventsQueueName,
                EventPatternNotification.PAYMENT_IN_PROGRESS_NOTIFICATION
        );

        log.info("✅ [SUBSCRIPTION] Notification sent to user | userId={} | userEmail={}", 
                savedInvoice.getUserId(), savedInvoice.getCustomerEmail());

        // Return the created invoice
        Map<String, Object> response = new HashMap<>();
        response.put("status", subscription.getStatus());
        response.put("message", "New subscription created successfully.");
        response.put("data", savedInvoice);

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        log.info("🎉 [SUBSCRIPTION] Subscription creation completed | userId={} | invoiceId={} | customerId={} | duration={}ms", 
                savedInvoice.getUserId(), savedInvoice.getId(), savedInvoice.getCustomerId(), durationMs);

        return response;
    }

    /**
     * Creates a one-time payment and saves the corresponding invoice.
     *
     * @param paymentToCreate The data required to create the one-time payment.
     * @return A map containing the status, message, and saved invoice data.
     * @throws StripeException if any Stripe operation fails.
     */
    public Map<String, Object> createOneTimePaymentAndSaveInvoice(PaymentDto paymentToCreate) throws StripeException {
        Instant startTime = Instant.now();
        
        // Set tracking context
        CorrelationContext.setUserId(paymentToCreate.getUserId());
        
        log.info("🔄 [PAYMENT] Starting one-time payment | userId={} | email={} | currency={} | jobId={}", 
                paymentToCreate.getUserId(), paymentToCreate.getEmail(), paymentToCreate.getCurrency(), 
                paymentToCreate.getJobId() != null ? paymentToCreate.getJobId() : "NOT_PROVIDED");
        
        if (paymentToCreate.getJobId() != null) {
            log.info("📌 [PAYMENT-JOB] Job ID detected in payment request | jobId={} | userId={}", 
                    paymentToCreate.getJobId(), paymentToCreate.getUserId());
        } else {
            log.warn("⚠️ [PAYMENT-JOB] No job ID provided in payment request | userId={} | This payment will not be linked to a specific job", 
                    paymentToCreate.getUserId());
        }

        // Get or create customer in Stripe
        com.stripe.model.Customer customer;
        try {
            // Try to find existing customer by email
            Map<String, Object> params = new HashMap<>();
            params.put("email", paymentToCreate.getEmail());
            params.put("limit", 1);
            
            com.stripe.model.CustomerCollection customers = com.stripe.model.Customer.list(params);
            
            if (customers.getData().isEmpty()) {
                // Create new customer if not found
                customer = stripeService.createCustomer(
                        paymentToCreate.getEmail(),
                        paymentToCreate.getPaymentMethod()
                );
                CorrelationContext.setCustomerId(customer.getId());
                log.info("✅ [PAYMENT] Created new Stripe customer | customerId={} | email={}", 
                        customer.getId(), customer.getEmail());
            } else {
                customer = customers.getData().get(0);
                CorrelationContext.setCustomerId(customer.getId());
                log.info("✅ [PAYMENT] Using existing Stripe customer | customerId={} | email={}", 
                        customer.getId(), customer.getEmail());
            }
        } catch (StripeException e) {
            log.error("Error finding/creating customer", e);
            throw e;
        }

        // Calculate total amount from items by retrieving price information
        long totalAmount = 0;
        JsonNode items = paymentToCreate.getItems();
        
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                String priceId = item.has("price") ? item.get("price").asText() : null;
                if (priceId != null) {
                    Price price = stripeService.getPrice(priceId);
                    Long unitAmount = price.getUnitAmount();
                    if (unitAmount != null) {
                        totalAmount += unitAmount;
                    }
                }
            }
        }

        log.info("💰 [PAYMENT] Total amount calculated | amount={} {} | itemCount={}", 
                totalAmount, paymentToCreate.getCurrency(), 
                items != null && items.isArray() ? items.size() : 0);

        // Create and confirm payment intent
        PaymentIntent paymentIntent = stripeService.createPaymentIntent(
                customer.getId(),
                paymentToCreate.getPaymentMethod(),
                totalAmount,
                paymentToCreate.getCurrency()
        );
        CorrelationContext.setPaymentIntentId(paymentIntent.getId());
        log.info("✅ [PAYMENT] Payment intent created and confirmed | paymentIntentId={} | status={} | amount={}", 
                paymentIntent.getId(), paymentIntent.getStatus(), paymentIntent.getAmount());

        // Prepare invoice to create
        Invoice invoiceToCreate = new Invoice();
        invoiceToCreate.setUserId(paymentToCreate.getUserId());
        invoiceToCreate.setCustomerId(customer.getId());
        invoiceToCreate.setCustomerEmail(customer.getEmail());
        invoiceToCreate.setItems(paymentToCreate.getItems());
        invoiceToCreate.setSubscriptionId(null); // No subscription for one-time payments
        invoiceToCreate.setStatus(paymentIntent.getStatus());
        invoiceToCreate.setLastPaymentIntentId(paymentIntent.getId());
        invoiceToCreate.setInvoiceIdProvidedByStripe(paymentIntent.getId()); // Use payment intent ID as invoice ID
        invoiceToCreate.setJobId(paymentToCreate.getJobId()); // Set job ID if provided

        // Create new invoice in DB from payment information
        Invoice savedInvoice = saveOrUpdate(invoiceToCreate);
        CorrelationContext.setInvoiceId(savedInvoice.getId().toString());
        
        log.info("✅ [PAYMENT] Saved invoice to DB | invoiceId={} | userId={} | customerId={} | status={} | jobId={}", 
                savedInvoice.getId(), savedInvoice.getUserId(), savedInvoice.getCustomerId(), 
                savedInvoice.getStatus(), savedInvoice.getJobId() != null ? savedInvoice.getJobId() : "NULL");
        
        if (savedInvoice.getJobId() != null) {
            log.info("📌 [PAYMENT-JOB] Invoice linked to job | invoiceId={} | jobId={}", 
                    savedInvoice.getId(), savedInvoice.getJobId());
        }

        boolean isActive = paymentIntent.getStatus().equals("succeeded");
        log.info("🛰 [PAYMENT->PLAN] Initiating plan creation | userId={} | invoiceId={} | jobId={} | paymentStatus={} | isActive={}", 
                paymentToCreate.getUserId(), savedInvoice.getId(), 
                savedInvoice.getJobId() != null ? savedInvoice.getJobId() : "NULL",
                paymentIntent.getStatus(), isActive);

        // Send message to RabbitMQ service to create a plan for the related invoice
        Map<String, Object> planData = new HashMap<>();
        planData.put("userId", paymentToCreate.getUserId());
        planData.put("invoiceId", savedInvoice.getId());
        planData.put("description", extractDescription(savedInvoice.getItems()));
        planData.put("items", savedInvoice.getItems());
        planData.put("isActive", isActive);
        planData.put("status", paymentIntent.getStatus());
        planData.put("durationInDays", 30);
        if (savedInvoice.getJobId() != null) {
            planData.put("jobId", savedInvoice.getJobId());
            log.info("📌 [PAYMENT->PLAN-JOB] Including job ID in RabbitMQ message | jobId={} | invoiceId={}", 
                    savedInvoice.getJobId(), savedInvoice.getId());
        } else {
            log.warn("⚠️ [PAYMENT->PLAN-JOB] No job ID to include in RabbitMQ message | invoiceId={}", 
                    savedInvoice.getId());
        }
        if (paymentToCreate.getMetadata() != null) {
            planData.put("metadata", paymentToCreate.getMetadata());
            log.info("📦 [PAYMENT->PLAN] Including metadata in RabbitMQ message | invoiceId={} | metadata={}", 
                    savedInvoice.getId(), paymentToCreate.getMetadata().toString());
        }

        rabbitMQService.sendDirectMessage(planData, plansToCreateQueueName);

        log.info("✅ [PAYMENT->PLAN] Plan creation message sent | userId={} | invoiceId={} | jobId={} | isActive={}", 
                paymentToCreate.getUserId(), savedInvoice.getId(), 
                savedInvoice.getJobId() != null ? savedInvoice.getJobId() : "NULL", isActive);

        // Prepare notification message for the user
        NotificationDto notificationMessage = new NotificationDto();
        notificationMessage.setUserId(savedInvoice.getUserId());
        notificationMessage.setUserEmail(savedInvoice.getCustomerEmail());
        notificationMessage.setNotificationType("payment");
        
        if (paymentIntent.getStatus().equals("succeeded")) {
            notificationMessage.setTopic("Payment successful");
            notificationMessage.setBody("Your payment has been processed successfully.");
            notificationMessage.setEventType(EventPatternNotification.PAYMENT_RECEIVED_NOTIFICATION.getPattern());
        } else {
            notificationMessage.setTopic("Payment processing");
            notificationMessage.setBody("Your payment is being processed.");
            notificationMessage.setEventType(EventPatternNotification.PAYMENT_IN_PROGRESS_NOTIFICATION.getPattern());
        }
        
        notificationMessage.setRead(false);
        notificationMessage.setCreatedAt(Instant.now());

        // Send message to RabbitMQ service to notify the user about the payment status
        rabbitMQService.createMessageMQService(
                notificationMessage,
                notificationEventsQueueName,
                paymentIntent.getStatus().equals("succeeded") 
                    ? EventPatternNotification.PAYMENT_RECEIVED_NOTIFICATION
                    : EventPatternNotification.PAYMENT_IN_PROGRESS_NOTIFICATION
        );

        log.info("✅ [PAYMENT] Notification sent to user | userId={} | userEmail={}", 
                savedInvoice.getUserId(), savedInvoice.getCustomerEmail());

        // Return the created invoice
        Map<String, Object> response = new HashMap<>();
        response.put("status", paymentIntent.getStatus());
        response.put("message", "One-time payment processed successfully.");
        response.put("data", savedInvoice);
        response.put("paymentIntentId", paymentIntent.getId());

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        log.info("🎉 [PAYMENT] One-time payment completed | userId={} | invoiceId={} | paymentIntentId={} | status={} | duration={}ms", 
                savedInvoice.getUserId(), savedInvoice.getId(), paymentIntent.getId(), paymentIntent.getStatus(), durationMs);

        return response;
    }

    /**
     * Handles incoming Stripe webhook events.
     *
     * @param payload The raw body of the webhook request.
     * @param signature The Stripe signature to verify the webhook's authenticity.
     * @throws Exception if webhook processing fails.
     */
    public void handleWebhook(String payload, String signature) throws Exception {
        log.info("ℹ️ Webhook received with signature");
        log.info("🔄 Loading webhook event...");

        Event event = stripeService.constructEvent(payload, signature);
        log.info("✅ Webhook event loaded: {}", event.getId());

        log.info("🔄 Processing webhook from Stripe with the related event type");
        processEvent(event);

        log.info("🏁 Webhook processed ended for event type: {} with event ID: {}", event.getType(), event.getId());
    }

    /**
     * Extracts description from items JsonNode.
     */
    private String extractDescription(JsonNode items) {
        StringBuilder description = new StringBuilder();
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                if (item.has("price")) {
                    description.append(item.get("price").asText());
                }
            }
        }
        return description.toString();
    }

    /**
     * Handles the 'invoice.created' event from Stripe.
     */
    private void handleInvoiceCreated(Event event) {
        log.warn("⚠️ No implementation for invoice created");
        // Add your logic for handling invoice creation
    }

    /**
     * Handles the 'invoice.updated' event from Stripe.
     */
    private void handleInvoiceUpdated(Event event) {
        log.warn("⚠️ No implementation for invoice updated");
        // Add your logic for handling invoice updates
    }

    /**
     * Handles the 'payment_intent.succeeded' event from Stripe, updating the corresponding invoice status
     * and sending a message to RabbitMQ to update the status of related plans.
     */
    private void handlePaymentIntentSucceeded(Event event) {
        Instant startTime = Instant.now();
        
        log.info("🔔 [WEBHOOK] Processing payment_intent.succeeded | eventId={}", event.getId());

        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize PaymentIntent"));

        String customerId = paymentIntent.getCustomer();
        String invoiceIdProvidedByStripe = paymentIntent.getInvoice();
        
        CorrelationContext.setCustomerId(customerId);
        CorrelationContext.setPaymentIntentId(paymentIntent.getId());

        log.info("🔍 [WEBHOOK] Looking up invoice | customerId={} | stripeInvoiceId={} | paymentIntentId={} | amount={}",
                customerId, invoiceIdProvidedByStripe, paymentIntent.getId(), paymentIntent.getAmount());

        Optional<Invoice> invoiceOptional = invoiceRepository.findByCustomerIdAndInvoiceIdProvidedByStripe(
                customerId, invoiceIdProvidedByStripe);

        if (invoiceOptional.isEmpty()) {
            log.error("❌ [WEBHOOK] Invoice not found in DB | customerId={} | stripeInvoiceId={} | paymentIntentId={}",
                    customerId, invoiceIdProvidedByStripe, paymentIntent.getId());
            return;
        }

        Invoice invoiceFound = invoiceOptional.get();
        CorrelationContext.setUserId(invoiceFound.getUserId());
        CorrelationContext.setInvoiceId(invoiceFound.getId().toString());
        
        log.info("✅ [WEBHOOK] Invoice found in DB | invoiceId={} | userId={} | currentStatus={} | jobId={}",
                invoiceFound.getId(), invoiceFound.getUserId(), invoiceFound.getStatus(),
                invoiceFound.getJobId() != null ? invoiceFound.getJobId() : "NULL");
        
        if (invoiceFound.getJobId() != null) {
            log.info("📌 [WEBHOOK-JOB] Invoice is linked to job | invoiceId={} | jobId={}", 
                    invoiceFound.getId(), invoiceFound.getJobId());
        }

        // Update invoice status
        String oldStatus = invoiceFound.getStatus();
        invoiceFound.setStatus(paymentIntent.getStatus());
        
        log.info("🔄 [WEBHOOK] Updating invoice status in DB | invoiceId={} | oldStatus={} | newStatus={}",
                invoiceFound.getId(), oldStatus, paymentIntent.getStatus());

        invoiceRepository.save(invoiceFound);
        
        log.info("✅ [WEBHOOK] Invoice status updated in DB | invoiceId={} | status={}",
                invoiceFound.getId(), paymentIntent.getStatus());

        log.info("🛰 [WEBHOOK->PLAN] Initiating plan status update | userId={} | invoiceId={} | jobId={} | newStatus={} | isActive=true", 
                invoiceFound.getUserId(), invoiceFound.getId(), 
                invoiceFound.getJobId() != null ? invoiceFound.getJobId() : "NULL", paymentIntent.getStatus());

        // Send message to RabbitMQ service to update the status of the existing related plan
        Map<String, Object> statusUpdateData = new HashMap<>();
        statusUpdateData.put("userId", invoiceFound.getUserId());
        statusUpdateData.put("invoiceId", invoiceFound.getId());
        statusUpdateData.put("description", extractDescription(invoiceFound.getItems()));
        statusUpdateData.put("items", invoiceFound.getItems());
        statusUpdateData.put("isActive", true);
        statusUpdateData.put("status", paymentIntent.getStatus());
        if (invoiceFound.getJobId() != null) {
            statusUpdateData.put("jobId", invoiceFound.getJobId());
            log.info("📌 [WEBHOOK->PLAN-JOB] Including job ID in status update message | jobId={} | invoiceId={}", 
                    invoiceFound.getJobId(), invoiceFound.getId());
        } else {
            log.warn("⚠️ [WEBHOOK->PLAN-JOB] No job ID to include in status update message | invoiceId={}", 
                    invoiceFound.getId());
        }

        rabbitMQService.sendDirectMessage(statusUpdateData, invoiceStatusOnRelatedPlansQueueName);

        log.info("✅ [WEBHOOK->PLAN] Plan status update message sent | userId={} | invoiceId={} | jobId={} | isActive=true", 
                invoiceFound.getUserId(), invoiceFound.getId(), 
                invoiceFound.getJobId() != null ? invoiceFound.getJobId() : "NULL");

        // Prepare notification message for the user
        NotificationDto notificationMessage = new NotificationDto();
        notificationMessage.setUserId(invoiceFound.getUserId());
        notificationMessage.setUserEmail(invoiceFound.getCustomerEmail());
        notificationMessage.setNotificationType("payment");
        notificationMessage.setTopic("Payment received");
        notificationMessage.setBody("Your payment has been received successfully.");
        notificationMessage.setRead(false);
        notificationMessage.setEventType(EventPatternNotification.PAYMENT_RECEIVED_NOTIFICATION.getPattern());
        notificationMessage.setCreatedAt(Instant.now());

        // Send message to RabbitMQ service to notify the user about the invoice status update
        rabbitMQService.createMessageMQService(
                notificationMessage,
                notificationEventsQueueName,
                EventPatternNotification.PAYMENT_RECEIVED_NOTIFICATION
        );

        log.info("✅ [WEBHOOK] Notification sent to user | userId={} | userEmail={}", 
                invoiceFound.getUserId(), invoiceFound.getCustomerEmail());
        
        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        log.info("🎉 [WEBHOOK] Payment intent succeeded processing completed | userId={} | invoiceId={} | paymentIntentId={} | duration={}ms",
                invoiceFound.getUserId(), invoiceFound.getId(), paymentIntent.getId(), durationMs);
    }

    /**
     * Processes a Stripe event by delegating it to the appropriate handler based on the event type.
     */
    private void processEvent(Event event) {
        log.info("🚚 Processing event type: {}", event.getType());
        Consumer<Event> handler = eventHandlers.get(event.getType());

        if (handler != null) {
            handler.accept(event);
        } else {
            log.warn("⚠️ Unhandled event type: {}", event.getType());
        }
    }
}
