package io.github.bluething.playground.java.ingestionservice.producer;

import io.github.bluething.playground.java.ingestionservice.config.IngestionKafkaProperties;
import io.github.bluething.playground.java.ingestionservice.model.LocationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaLocationEventProducerTest {
    @Mock
    private KafkaTemplate<String, LocationEvent> kafkaTemplate;
    @Mock
    private IngestionKafkaProperties properties;
    @Mock
    private CompletableFuture<SendResult<String, LocationEvent>> future;
    @InjectMocks
    private KafkaLocationEventProducer producer;

    @Test
    void send_shouldPublishToKafka() {
        when(properties.getTopic()).thenReturn("testTopic");

        LocationEvent event = new LocationEvent(
                "user1",
                12.34,
                56.78,
                Instant.now(),
                Map.of("k", "v"));

        when(kafkaTemplate.send("testTopic", event.userId(), event)).thenReturn(future);

        producer.send(event);

        verify(kafkaTemplate).send("testTopic", event.userId(), event);
    }

}