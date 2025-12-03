package com.remotejob.invoiceservice.amqp;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.queue.invoice-status-on-related-plans}")
    private String invoiceStatusOnRelatedPlansQueueName;

    @Value("${rabbitmq.queue.plans-to-create}")
    private String plansToCreateQueueName;

    @Value("${rabbitmq.queue.notification-events}")
    private String notificationEventsQueueName;

    @Bean
    public Queue invoiceStatusOnRelatedPlansQueue() {
        return new Queue(invoiceStatusOnRelatedPlansQueueName, true);
    }

    @Bean
    public Queue plansToCreateQueue() {
        return new Queue(plansToCreateQueueName, true);
    }

    @Bean
    public Queue notificationEventsQueue() {
        return new Queue(notificationEventsQueueName, true);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
