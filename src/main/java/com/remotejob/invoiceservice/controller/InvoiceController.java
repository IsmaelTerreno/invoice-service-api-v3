package com.remotejob.invoiceservice.controller;

import com.remotejob.invoiceservice.dto.InvoiceDto;
import com.remotejob.invoiceservice.dto.ResponseAPI;
import com.remotejob.invoiceservice.dto.SubscriptionDto;
import com.remotejob.invoiceservice.entity.Invoice;
import com.remotejob.invoiceservice.mapper.InvoiceMapper;
import com.remotejob.invoiceservice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for invoice operations.
 */
@RestController
@RequestMapping("/api/v1/invoice")
@Tag(name = "Invoice", description = "Invoice management APIs")
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;

    public InvoiceController(InvoiceService invoiceService, InvoiceMapper invoiceMapper) {
        this.invoiceService = invoiceService;
        this.invoiceMapper = invoiceMapper;
    }

    /**
     * Finds and returns all invoices.
     *
     * @return A list of InvoiceDto objects.
     */
    @Operation(
            summary = "Finds all invoices",
            description = "Retrieves all invoices from the database"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of invoices")
    @GetMapping("/all")
    public ResponseEntity<List<InvoiceDto>> findAll() {
        log.info("GET /api/v1/invoice/all - Fetching all invoices");
        List<Invoice> invoices = invoiceService.findAll();
        List<InvoiceDto> invoiceDtos = invoices.stream()
                .map(invoiceMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(invoiceDtos);
    }

    /**
     * Creates a subscription in Stripe and saves the invoice in the database.
     *
     * @param subscriptionToCreate The subscription details to be created.
     * @return The result of the subscription creation process, including the saved invoice.
     */
    @Operation(
            summary = "Creates a subscription in Stripe and saves the invoice",
            description = "Creates a new subscription in Stripe and stores the invoice information in the database"
    )
    @ApiResponse(responseCode = "200", description = "Subscription created successfully")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PostMapping("/create-subscription")
    public ResponseEntity<ResponseAPI<Map<String, Object>>> createSubscription(
            @Valid @RequestBody SubscriptionDto subscriptionToCreate) {
        try {
            log.info("POST /api/v1/invoice/create-subscription - Creating subscription for user: {}",
                    subscriptionToCreate.getUserId());

            Map<String, Object> result = invoiceService.createSubscriptionAndSaveInvoice(subscriptionToCreate);

            ResponseAPI<Map<String, Object>> response = ResponseAPI.<Map<String, Object>>builder()
                    .status("succeeded")
                    .message("Subscription created successfully.")
                    .data(result)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating subscription", e);

            ResponseAPI<Map<String, Object>> errorResponse = ResponseAPI.<Map<String, Object>>builder()
                    .status("error")
                    .message(e.getMessage())
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Handles the Stripe webhook and updates the invoice status in the database.
     * Also sends a message to the RabbitMQ service to update the status of the existing related plan.
     *
     * @param payload The raw body of the webhook request.
     * @param signature The Stripe signature header for validating the webhook request.
     * @return HTTP 200 if successful, HTTP 400 if validation fails.
     */
    @Operation(
            summary = "Handles Stripe webhook events",
            description = "Processes Stripe webhook events, updates invoice status, and sends messages to RabbitMQ"
    )
    @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    @ApiResponse(responseCode = "400", description = "Webhook validation failed")
    @PostMapping("/stripe_webhooks")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("stripe-signature") String signature) {
        try {
            log.info("POST /api/v1/invoice/stripe_webhooks - Handling Stripe webhook");
            invoiceService.handleWebhook(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Webhook Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook Error: " + e.getMessage());
        }
    }
}
