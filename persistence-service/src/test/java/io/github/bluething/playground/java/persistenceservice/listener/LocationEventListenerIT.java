package io.github.bluething.playground.java.persistenceservice.listener;

import io.github.bluething.playground.java.persistenceservice.config.KafkaProducerConfig;
import io.github.bluething.playground.java.persistenceservice.model.LocationEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"location-events"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "ingestion.kafka.topic=location-events"
})
@Import(KafkaProducerConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocationEventListenerIT {
    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaTemplate<String, LocationEvent> kafkaTemplate;

    @MockitoSpyBean
    private LocationEventListener listener;

    @Test
    void whenSendingMessage_thenListenerReceivesIt() {
        // Given
        LocationEvent event = new LocationEvent(
                "user1",
                12.34,
                56.78,
                Instant.now(),
                Map.of("source", "test")
        );

        // When
        kafkaTemplate.send("location-events", event.userId(), event);

        // Then
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(listener).listen(argThat(received ->
                        received.userId().equals("user1") &&
                                received.latitude() == 12.34 &&
                                received.longitude() == 56.78
                ))
        );
    }
}