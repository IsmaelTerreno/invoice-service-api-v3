package com.remotejob.invoiceservice.amqp;

/**
 * Enum for RabbitMQ event pattern types.
 */
public enum EventPatternNotification {
    INVOICE_STATUS_UPDATE("invoice-status-update"),
    PLANS_TO_CREATE("plans-to-create"),
    PAYMENT_RECEIVED_NOTIFICATION("payment-received-notification"),
    PAYMENT_IN_PROGRESS_NOTIFICATION("payment-in-progress-notification"),
    PLAN_IS_ACTIVE_NOTIFICATION("plan-is-active-notification");

    private final String pattern;

    EventPatternNotification(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }
}
