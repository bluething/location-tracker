package io.github.bluething.playground.java.ingestionservice.config;

import io.github.bluething.playground.java.ingestionservice.model.LocationEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
class KafkaProducerConfig {
    private final KafkaProperties kafkaProperties;

    KafkaProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ProducerFactory<String, LocationEvent> getProducerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

        // Ensure durability & ordering
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        // Throughput tuning
        props.put(ProducerConfig.LINGER_MS_CONFIG, 20);          // wait up to 20ms to batch
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 64 * 1024); // 64KB batch
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 64 * 1024 * 1024L); // 64MB buffer
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // reduce payload size

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, LocationEvent> getKafkaTemplate(ProducerFactory<String, LocationEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
