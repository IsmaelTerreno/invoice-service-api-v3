package com.remotejob.planservice.amqp;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.amqp.queues.invoice-status-updates}")
    private String invoiceStatusUpdatesQueueName;

    @Value("${app.amqp.queues.plans-to-create}")
    private String plansToCreateQueueName;

    @Bean
    public Queue invoiceStatusUpdatesQueue() {
        return new Queue(invoiceStatusUpdatesQueueName, true);
    }

    @Bean
    public Queue plansToCreateQueue() {
        return new Queue(plansToCreateQueueName, true);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
