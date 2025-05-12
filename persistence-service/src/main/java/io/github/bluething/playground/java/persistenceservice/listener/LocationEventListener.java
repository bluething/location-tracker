package io.github.bluething.playground.java.persistenceservice.listener;

import io.github.bluething.playground.java.persistenceservice.model.LocationEvent;
import io.github.bluething.playground.java.persistenceservice.repository.LocationEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class LocationEventListener {
    private final LocationEventRepository repository;

    LocationEventListener(LocationEventRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${ingestion.kafka.topic}", containerFactory = "kafkaListenerContainerFactory")
    void listen(LocationEvent locationEvent) {
        repository.save(locationEvent);
    }
}
