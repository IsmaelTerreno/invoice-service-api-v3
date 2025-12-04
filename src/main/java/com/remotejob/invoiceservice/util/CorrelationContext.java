package com.remotejob.invoiceservice.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for managing correlation context in logs.
 * Uses SLF4J's MDC (Mapped Diagnostic Context) to add contextual information to logs.
 */
public class CorrelationContext {

    private static final String CORRELATION_ID = "correlationId";
    private static final String USER_ID = "userId";
    private static final String INVOICE_ID = "invoiceId";
    private static final String CUSTOMER_ID = "customerId";
    private static final String PAYMENT_INTENT_ID = "paymentIntentId";
    private static final String SUBSCRIPTION_ID = "subscriptionId";

    private CorrelationContext() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generates and sets a new correlation ID.
     *
     * @return The generated correlation ID.
     */
    public static String initCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID, correlationId);
        return correlationId;
    }

    /**
     * Sets an existing correlation ID.
     *
     * @param correlationId The correlation ID to set.
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.isEmpty()) {
            MDC.put(CORRELATION_ID, correlationId);
        }
    }

    /**
     * Gets the current correlation ID.
     *
     * @return The current correlation ID or null if not set.
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }

    /**
     * Sets the user ID in the context.
     *
     * @param userId The user ID.
     */
    public static void setUserId(String userId) {
        if (userId != null && !userId.isEmpty()) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * Sets the invoice ID in the context.
     *
     * @param invoiceId The invoice ID.
     */
    public static void setInvoiceId(String invoiceId) {
        if (invoiceId != null && !invoiceId.isEmpty()) {
            MDC.put(INVOICE_ID, invoiceId);
        }
    }

    /**
     * Sets the customer ID in the context.
     *
     * @param customerId The customer ID.
     */
    public static void setCustomerId(String customerId) {
        if (customerId != null && !customerId.isEmpty()) {
            MDC.put(CUSTOMER_ID, customerId);
        }
    }

    /**
     * Sets the payment intent ID in the context.
     *
     * @param paymentIntentId The payment intent ID.
     */
    public static void setPaymentIntentId(String paymentIntentId) {
        if (paymentIntentId != null && !paymentIntentId.isEmpty()) {
            MDC.put(PAYMENT_INTENT_ID, paymentIntentId);
        }
    }

    /**
     * Sets the subscription ID in the context.
     *
     * @param subscriptionId The subscription ID.
     */
    public static void setSubscriptionId(String subscriptionId) {
        if (subscriptionId != null && !subscriptionId.isEmpty()) {
            MDC.put(SUBSCRIPTION_ID, subscriptionId);
        }
    }

    /**
     * Sets invoice and plan tracking context.
     *
     * @param userId The user ID.
     * @param invoiceId The invoice ID.
     */
    public static void setInvoicePlanContext(String userId, String invoiceId) {
        setUserId(userId);
        setInvoiceId(invoiceId);
    }

    /**
     * Clears all MDC context.
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * Clears specific context key.
     *
     * @param key The key to remove.
     */
    public static void remove(String key) {
        MDC.remove(key);
    }

    /**
     * Creates a log prefix with tracking information.
     *
     * @return A formatted string with tracking context.
     */
    public static String getTrackingInfo() {
        StringBuilder info = new StringBuilder("[");
        String correlationId = MDC.get(CORRELATION_ID);
        String userId = MDC.get(USER_ID);
        String invoiceId = MDC.get(INVOICE_ID);

        if (correlationId != null) {
            info.append("correlation=").append(correlationId);
        }
        if (userId != null) {
            if (info.length() > 1) info.append(", ");
            info.append("userId=").append(userId);
        }
        if (invoiceId != null) {
            if (info.length() > 1) info.append(", ");
            info.append("invoiceId=").append(invoiceId);
        }

        info.append("]");
        return info.length() > 2 ? info.toString() : "";
    }
}

