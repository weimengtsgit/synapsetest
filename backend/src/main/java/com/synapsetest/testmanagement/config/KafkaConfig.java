package com.synapsetest.testmanagement.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Configuration
 * Configures Kafka topics for messaging
 */
@Configuration
public class KafkaConfig {

    public static final String TASK_EVENTS_TOPIC = "test-task-events";
    public static final String TEST_RESULTS_TOPIC = "test-results";
    public static final String AI_RECOMMENDATIONS_TOPIC = "ai-recommendations";

    @Bean
    public NewTopic taskEventsTopic() {
        return TopicBuilder.name(TASK_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic testResultsTopic() {
        return TopicBuilder.name(TEST_RESULTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic aiRecommendationsTopic() {
        return TopicBuilder.name(AI_RECOMMENDATIONS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
