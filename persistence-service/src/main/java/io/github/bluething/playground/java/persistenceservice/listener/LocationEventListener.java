package io.github.bluething.playground.java.persistenceservice.listener;

import io.github.bluething.playground.java.persistenceservice.model.LocationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class LocationEventListener {
    @KafkaListener(topics = "${ingestion.kafka.topic}", containerFactory = "kafkaListenerContainerFactory")
    void listen(LocationEvent locationEvent) {

    }
}
