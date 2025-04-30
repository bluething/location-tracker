package io.github.bluething.playground.java.ingestionservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bluething.playground.java.ingestionservice.model.LocationEvent;
import io.github.bluething.playground.java.ingestionservice.model.RawLocation;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"location-events"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "ingestion.kafka.topic=location-events"
})
class LocationWebSocketHandlerIT {
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @LocalServerPort
    private int port;

    private Consumer<String, LocationEvent> consumer;
    private WebSocketSession session;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        // Create consumer properties, trusting our model package for JSON deserialization
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "false", embeddedKafkaBroker);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "io.github.bluething.playground.java.ingestionservice.model");
        // Build a consumer factory and subscribe to the topic
        DefaultKafkaConsumerFactory<String, LocationEvent> cf = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(LocationEvent.class, false)
        );
        consumer = cf.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "location-events");

        // Initialize WebSocket client
        StandardWebSocketClient client = new StandardWebSocketClient();
        CompletableFuture<WebSocketSession> future = client.execute(
                new TextWebSocketHandler(),
                "ws://localhost:" + port + "/ws/location"
        );
        session = future.get(2, TimeUnit.SECONDS);
    }

    @Test
    void whenSendLocation_overWebSocket_thenMessageAppearsInKafka() throws Exception {
        RawLocation raw = new RawLocation("user1", 12.34, 56.78);
        String payload = objectMapper.writeValueAsString(raw);

        session.sendMessage(new TextMessage(payload));

        ConsumerRecords<String, LocationEvent> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
        assertEquals(1, records.count());
        ConsumerRecord<String, LocationEvent> record = records.iterator().next();

        assertEquals("user1", record.key());
        LocationEvent event = record.value();
        assertEquals(raw.latitude(), event.latitude());
        assertEquals(raw.longitude(), event.longitude());
        assertEquals(raw.userId(), event.userId());
        assertTrue(event.labels().containsKey("source"));
        assertTrue(event.labels().containsKey("sessionId"));
    }

    @AfterEach
    void tearDown() throws Exception {
        session.close();
        consumer.close();
    }
}