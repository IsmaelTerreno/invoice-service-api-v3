package com.remotejob.invoiceservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.remotejob.invoiceservice.amqp.EventPatternNotification;
import com.remotejob.invoiceservice.dto.NotificationDto;
import com.remotejob.invoiceservice.dto.SubscriptionDto;
import com.remotejob.invoiceservice.entity.Invoice;
import com.remotejob.invoiceservice.repository.InvoiceRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        log.info("🔄 Processing event type: Create subscription and save invoice");

        // Create new customer in Stripe
        com.stripe.model.Customer customer = stripeService.createCustomer(
                subscriptionToCreate.getEmail(),
                subscriptionToCreate.getPaymentMethod()
        );
        log.info("✅ Created Stripe customer: {}", customer.getId());

        // Create new subscription in Stripe
        Subscription subscription = stripeService.createSubscription(
                customer.getId(),
                subscriptionToCreate.getItems()
        );
        log.info("✅ Created Stripe subscription: {}", subscription.getId());

        // Get the latest invoice and payment intent
        com.stripe.model.Invoice latestInvoice = (com.stripe.model.Invoice) subscription.getLatestInvoiceObject();
        PaymentIntent paymentIntent = (PaymentIntent) latestInvoice.getPaymentIntentObject();

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

        // Create new invoice in DB from Stripe subscription information
        Invoice savedInvoice = saveOrUpdate(invoiceToCreate);
        log.info("✅ Created in DB the invoice with customer ID: {} and with invoice ID: {}",
                savedInvoice.getUserId(), savedInvoice.getId());

        // Confirm payment intent to complete the subscription
        stripeService.confirmPaymentIntent(
                paymentIntent.getId(),
                subscriptionToCreate.getPaymentMethod()
        );
        log.info("✅ Confirmed the payment intent with customer ID: {} and with invoice ID: {}",
                savedInvoice.getUserId(), savedInvoice.getId());

        log.info("🛰 Sending message to RabbitMQ to create the related plan with customer ID: {} and with invoice ID: {}",
                subscriptionToCreate.getUserId(), savedInvoice.getId());

        // Send message to RabbitMQ service to create a plan for the related invoice
        Map<String, Object> planData = new HashMap<>();
        planData.put("userId", subscriptionToCreate.getUserId());
        planData.put("invoiceId", savedInvoice.getId());
        planData.put("description", extractDescription(savedInvoice.getItems()));
        planData.put("items", savedInvoice.getItems());
        planData.put("isActive", false);
        planData.put("status", subscription.getStatus());
        planData.put("durationInDays", 30);

        rabbitMQService.createMessageMQService(
                planData,
                invoiceStatusOnRelatedPlansQueueName,
                EventPatternNotification.PLANS_TO_CREATE
        );

        log.info("🚀 Sent message to RabbitMQ to create the related plan with customer ID: {} and with invoice ID: {}",
                subscriptionToCreate.getUserId(), savedInvoice.getId());

        // Prepare notification message for the user
        NotificationDto notificationMessage = new NotificationDto();
        notificationMessage.setUserId(savedInvoice.getUserId());
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

        log.info("🚀 Sent message to RabbitMQ the notification for customer ID: {} and with invoice ID: {}",
                savedInvoice.getUserId(), savedInvoice.getId());

        // Return the created invoice
        Map<String, Object> response = new HashMap<>();
        response.put("status", subscription.getStatus());
        response.put("message", "New subscription created successfully.");
        response.put("data", savedInvoice);

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
        log.info("🔄 Processing event type: Payment intent succeeded");

        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize PaymentIntent"));

        String customerId = paymentIntent.getCustomer();
        String invoiceIdProvidedByStripe = paymentIntent.getInvoice();

        log.info("🔄 Searching in DB the invoice status with customer ID: {} and with invoice ID: {}",
                customerId, invoiceIdProvidedByStripe);

        Optional<Invoice> invoiceOptional = invoiceRepository.findByCustomerIdAndInvoiceIdProvidedByStripe(
                customerId, invoiceIdProvidedByStripe);

        if (invoiceOptional.isEmpty()) {
            log.error("❌ Related invoice not found for the payment intent succeeded event with customer ID: {} and with invoice ID: {}",
                    customerId, invoiceIdProvidedByStripe);
            return;
        }

        Invoice invoiceFound = invoiceOptional.get();

        // Update invoice status
        invoiceFound.setStatus(paymentIntent.getStatus());
        log.info("🔄 Updating in DB the invoice status with customer ID: {} and with invoice ID: {} with payment status: {}",
                customerId, invoiceIdProvidedByStripe, paymentIntent.getStatus());

        invoiceRepository.save(invoiceFound);
        log.info("✅ Updated in DB the invoice status with customer ID: {} and with invoice ID: {} with payment status: {}",
                customerId, invoiceIdProvidedByStripe, paymentIntent.getStatus());

        log.info("🛰 Sending message to RabbitMQ to update the status of the related plan with customer ID: {} and with invoice ID: {}",
                invoiceFound.getUserId(), invoiceFound.getId());

        // Send message to RabbitMQ service to update the status of the existing related plan
        Map<String, Object> statusUpdateData = new HashMap<>();
        statusUpdateData.put("userId", invoiceFound.getUserId());
        statusUpdateData.put("invoiceId", invoiceFound.getId());
        statusUpdateData.put("description", extractDescription(invoiceFound.getItems()));
        statusUpdateData.put("items", invoiceFound.getItems());
        statusUpdateData.put("isActive", false);
        statusUpdateData.put("status", paymentIntent.getStatus());

        rabbitMQService.createMessageMQService(
                statusUpdateData,
                invoiceStatusOnRelatedPlansQueueName,
                EventPatternNotification.INVOICE_STATUS_UPDATE
        );

        log.info("🚀 Sent message to RabbitMQ to update the status of the related plan with customer ID: {} and with invoice ID: {}",
                invoiceFound.getUserId(), invoiceFound.getId());

        // Prepare notification message for the user
        NotificationDto notificationMessage = new NotificationDto();
        notificationMessage.setUserId(invoiceFound.getUserId());
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

        log.info("🚀 Sent message to RabbitMQ the notification for customer ID: {} and with invoice ID: {}",
                invoiceFound.getUserId(), invoiceFound.getId());
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
