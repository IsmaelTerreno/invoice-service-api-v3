package com.remotejob.invoiceservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.SubscriptionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Service for handling Stripe operations such as creating subscriptions,
 * customers, and confirming payment intents.
 */
@Service
@Slf4j
public class StripeService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Creates a new subscription for a given customer with specified items.
     *
     * @param customerId The unique identifier of the customer.
     * @param items A JsonNode representing the items to include in the subscription.
     * @return The newly created subscription object.
     * @throws StripeException if the subscription creation fails.
     */
    public Subscription createSubscription(String customerId, JsonNode items) throws StripeException {
        log.info("Creating subscription for customer: {}", customerId);

        SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .setPaymentSettings(
                        SubscriptionCreateParams.PaymentSettings.builder()
                                .setSaveDefaultPaymentMethod(SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                                .build()
                )
                .addExpand("latest_invoice.payment_intent")
                .addExpand("pending_setup_intent");

        // Add items from JsonNode
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                String priceId = item.has("price") ? item.get("price").asText() : null;
                if (priceId != null) {
                    paramsBuilder.addItem(
                            SubscriptionCreateParams.Item.builder()
                                    .setPrice(priceId)
                                    .build()
                    );
                }
            }
        }

        return Subscription.create(paramsBuilder.build());
    }

    /**
     * Creates a new customer in the Stripe system using the provided email and payment method.
     *
     * @param email The email address of the customer.
     * @param paymentMethod The payment method ID to be associated with the customer.
     * @return The newly created customer object.
     * @throws StripeException if the customer creation fails.
     */
    public Customer createCustomer(String email, String paymentMethod) throws StripeException {
        log.info("Creating Stripe customer with email: {}", email);

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setPaymentMethod(paymentMethod)
                .setInvoiceSettings(
                        CustomerCreateParams.InvoiceSettings.builder()
                                .setDefaultPaymentMethod(paymentMethod)
                                .build()
                )
                .build();

        return Customer.create(params);
    }

    /**
     * Confirms a PaymentIntent with the specified payment method.
     *
     * @param paymentIntentId The unique identifier of the PaymentIntent to confirm.
     * @param paymentMethodId The unique identifier of the payment method to use for confirmation.
     * @return The confirmed PaymentIntent object.
     * @throws StripeException if the payment intent confirmation fails.
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId) throws StripeException {
        log.info("Confirming payment intent: {}", paymentIntentId);

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

        PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                .setPaymentMethod(paymentMethodId)
                .build();

        return paymentIntent.confirm(params);
    }

    /**
     * Constructs a Stripe event object from the provided body and signature.
     *
     * @param payload The payload of the webhook event.
     * @param sigHeader The signature from the Stripe webhook headers.
     * @return The constructed Stripe event object.
     * @throws SignatureVerificationException if the signature verification fails.
     */
    public Event constructEvent(String payload, String sigHeader) throws SignatureVerificationException {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            String message = "Webhook secret not found, please review the configuration";
            log.error(message);
            throw new IllegalStateException(message);
        }

        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            String message = "Webhook Error: " + e.getMessage();
            log.error(message);
            throw e;
        }
    }
}
