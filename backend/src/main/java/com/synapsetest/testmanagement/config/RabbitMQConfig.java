package com.synapsetest.testmanagement.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration
 * Configures RabbitMQ exchanges and queues for low-latency messaging
 */
@Configuration
public class RabbitMQConfig {

    public static final String TASK_QUEUE = "test.task.queue";
    public static final String NOTIFICATION_QUEUE = "test.notification.queue";
    public static final String EXCHANGE = "test.management.exchange";

    @Bean
    public Queue taskQueue() {
        return new Queue(TASK_QUEUE, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding taskBinding(Queue taskQueue, TopicExchange exchange) {
        return BindingBuilder.bind(taskQueue).to(exchange).with("task.#");
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue).to(exchange).with("notification.#");
    }
}
