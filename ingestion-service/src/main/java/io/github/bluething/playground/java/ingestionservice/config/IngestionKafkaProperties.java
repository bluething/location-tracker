package io.github.bluething.playground.java.ingestionservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingestion.kafka")
public class IngestionKafkaProperties {
    /** Kafka topic to publish location events */
    private String topic;

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
}
