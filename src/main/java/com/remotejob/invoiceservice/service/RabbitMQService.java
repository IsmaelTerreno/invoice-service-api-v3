package com.remotejob.invoiceservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remotejob.invoiceservice.amqp.EventPatternNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

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
        try {
            log.info("Sending message to queue: {}", queue);
            rabbitTemplate.convertAndSend(queue, message);
            log.info("Message sent successfully to queue: {}", queue);
        } catch (Exception e) {
            log.error("Failed to send message to queue: {}", queue, e);
            throw new RuntimeException("Failed to send message to RabbitMQ", e);
        }
    }

    /**
     * Creates and sends a message to RabbitMQ with event pattern format.
     *
     * @param dataPayload The data payload to be sent.
     * @param queueName The name of the queue to send to.
     * @param eventPatternName The event pattern notification type.
     */
    public void createMessageMQService(Object dataPayload, String queueName, EventPatternNotification eventPatternName) {
        try {
            Map<String, Object> messageDetail = new HashMap<>();
            messageDetail.put("pattern", eventPatternName.getPattern());
            messageDetail.put("data", dataPayload);

            log.info("Creating message for queue: {} with pattern: {}", queueName, eventPatternName.getPattern());
            sendToQueue(queueName, messageDetail);
        } catch (Exception e) {
            log.error("Failed to create message for queue: {} with pattern: {}", queueName, eventPatternName.getPattern(), e);
            throw new RuntimeException("Failed to create and send message to RabbitMQ", e);
        }
    }
}
