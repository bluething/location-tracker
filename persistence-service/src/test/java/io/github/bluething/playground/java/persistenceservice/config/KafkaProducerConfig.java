package io.github.bluething.playground.java.persistenceservice.config;

import io.github.bluething.playground.java.persistenceservice.model.LocationEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;

@TestConfiguration
public class KafkaProducerConfig {
    @Bean
    public KafkaTemplate<String, LocationEvent> testKafkaTemplate(EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        DefaultKafkaProducerFactory<String, LocationEvent> factory =
                new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new JsonSerializer<>());
        return new KafkaTemplate<>(factory);
    }
}
