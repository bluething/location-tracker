package io.github.bluething.playground.java.ingestionservice.producer;

import io.github.bluething.playground.java.ingestionservice.model.LocationEvent;

public interface LocationEventProducer {
    void send(LocationEvent event);
}
