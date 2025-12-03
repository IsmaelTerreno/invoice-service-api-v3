package com.remotejob.invoiceservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remotejob.invoiceservice.amqp.EventPatternNotification;
import com.remotejob.invoiceservice.util.CorrelationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQService is responsible for managing RabbitMQ message publishing.
 * It provides methods for sending messages to specific queues.
 */
@Service
@Slf4j
public class RabbitMQService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitMQService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a message to a specified queue.
     *
     * @param queue The name of the queue to send the message to.
     * @param message The message object to be sent to the queue.
     */
    public void sendToQueue(String queue, Object message) {
        Instant startTime = Instant.now();
        String correlationId = CorrelationContext.getCorrelationId();
        
        try {
            log.info("📤 [RabbitMQ] Sending message | queue={} | correlationId={}", 
                    queue, correlationId);
            log.debug("📤 [RabbitMQ] Message payload: {}", message);
            
            rabbitTemplate.convertAndSend(queue, message);
            
            long durationMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            log.info("✅ [RabbitMQ] Message sent successfully | queue={} | correlationId={} | duration={}ms", 
                    queue, correlationId, durationMs);
        } catch (Exception e) {
            long durationMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            log.error("❌ [RabbitMQ] Failed to send message | queue={} | correlationId={} | duration={}ms | error={}", 
                    queue, correlationId, durationMs, e.getMessage(), e);
            throw new RuntimeException("Failed to send message to RabbitMQ: " + e.getMessage(), e);
        }
    }

    /**
     * Sends unwrapped data directly to a queue (for plan and invoice events).
     *
     * @param dataPayload The data payload to be sent.
     * @param queueName The name of the queue to send to.
     */
    public void sendDirectMessage(Object dataPayload, String queueName) {
        String trackingInfo = CorrelationContext.getTrackingInfo();
        
        try {
            log.info("🚀 [RabbitMQ] Preparing direct message {} | queue={}", trackingInfo, queueName);
            sendToQueue(queueName, dataPayload);
        } catch (Exception e) {
            log.error("❌ [RabbitMQ] Failed to send direct message {} | queue={} | error={}", 
                    trackingInfo, queueName, e.getMessage(), e);
            throw new RuntimeException("Failed to send direct message to RabbitMQ: " + e.getMessage(), e);
        }
    }

    /**
     * Creates and sends a message to RabbitMQ with event pattern format (for notifications).
     *
     * @param dataPayload The data payload to be sent.
     * @param queueName The name of the queue to send to.
     * @param eventPatternName The event pattern notification type.
     */
    public void createMessageMQService(Object dataPayload, String queueName, EventPatternNotification eventPatternName) {
        String trackingInfo = CorrelationContext.getTrackingInfo();
        
        try {
            Map<String, Object> messageDetail = new HashMap<>();
            messageDetail.put("pattern", eventPatternName.getPattern());
            messageDetail.put("data", dataPayload);

            log.info("🔔 [RabbitMQ] Creating notification message {} | queue={} | pattern={}", 
                    trackingInfo, queueName, eventPatternName.getPattern());
            sendToQueue(queueName, messageDetail);
        } catch (Exception e) {
            log.error("❌ [RabbitMQ] Failed to create notification message {} | queue={} | pattern={} | error={}", 
                    trackingInfo, queueName, eventPatternName.getPattern(), e.getMessage(), e);
            throw new RuntimeException("Failed to create and send message to RabbitMQ: " + e.getMessage(), e);
        }
    }
}
