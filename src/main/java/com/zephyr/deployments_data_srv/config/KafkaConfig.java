package com.zephyr.deployments_data_srv.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    /**
     * Declares a new Kafka topic. Spring Boot will automatically register/create 
     * this topic on the Kafka cluster when the application starts.
     */
    @Bean
    public NewTopic deploymentsTopic() {
        return TopicBuilder.name("deployments-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
