package com.remotejob.planservice.amqp;

import com.remotejob.planservice.amqp.dto.InvoiceStatusUpdateEvent;
import com.remotejob.planservice.amqp.dto.PlansToCreateEvent;
import com.remotejob.planservice.service.InvoiceWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventsListener {

    private final InvoiceWorkerService invoiceWorkerService;

    @RabbitListener(queues = "${app.amqp.queues.invoice-status-updates}")
    public void onInvoiceStatusUpdate(@Payload InvoiceStatusUpdateEvent event) {
        log.info("[AMQP] Received InvoiceStatusUpdateEvent");
        invoiceWorkerService.handleInvoiceStatusUpdate(event);
    }

    @RabbitListener(queues = "${app.amqp.queues.plans-to-create}")
    public void onPlansToCreate(@Payload PlansToCreateEvent event) {
        log.info("[AMQP] Received PlansToCreateEvent");
        invoiceWorkerService.handlePlansToCreate(event);
    }
}
