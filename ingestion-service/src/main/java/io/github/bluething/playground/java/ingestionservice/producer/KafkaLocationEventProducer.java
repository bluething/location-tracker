package io.github.bluething.playground.java.ingestionservice.producer;

import io.github.bluething.playground.java.ingestionservice.config.IngestionKafkaProperties;
import io.github.bluething.playground.java.ingestionservice.model.LocationEvent;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;

@Service
class KafkaLocationEventProducer implements LocationEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaLocationEventProducer.class.getName());
    private final KafkaTemplate<String, LocationEvent> kafkaTemplate;
    private final IngestionKafkaProperties props;

    public KafkaLocationEventProducer(KafkaTemplate<String, LocationEvent> kafkaTemplate, IngestionKafkaProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    @Override
    public void send(LocationEvent event) {
        logger.debug("Publishing to {}: {}", props.getTopic(), event);
        kafkaTemplate.send(props.getTopic(), event.userId(), event).whenComplete((result, ex) -> {
            if (ex == null) {
                logger.trace("Sent to partition {}", result.getRecordMetadata().partition());
            } else {
                logger.error("Sent to partition {} failed", result.getRecordMetadata().partition(), ex);
            }
        });
    }
}
