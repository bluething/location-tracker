package io.github.bluething.playground.java.persistenceservice.command;

import io.github.bluething.playground.java.persistenceservice.config.KafkaProducerConfig;
import io.github.bluething.playground.java.persistenceservice.service.LocationEventUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"location-events"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "ingestion.kafka.topic=location-events"
})
@Import(KafkaProducerConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocationCliCommandsIT {
    @Autowired
    private LocationCliCommands cliCommands;

    @MockitoSpyBean
    private LocationEventUseCase locationEventUseCase;

    @Test
    void shouldForwardParametersToQueryUserEvents() {
        cliCommands.queryUserEvents("user1", "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z");

        verify(locationEventUseCase).findByUser(
                eq("user1"),
                eq(Instant.parse("2024-01-01T00:00:00Z")),
                eq(Instant.parse("2024-01-02T00:00:00Z"))
        );
    }
}
