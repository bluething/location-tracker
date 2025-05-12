package io.github.bluething.playground.java.persistenceservice.service;

import io.github.bluething.playground.java.persistenceservice.model.LocationEvent;

import java.time.Instant;
import java.util.List;

public interface LocationEventUseCase {
    List<LocationEvent> findByUser(String userId, Instant from, Instant to);
    int countEventsInArea(double minLat, double maxLat, double minLon, double maxLon, Instant since);
}
