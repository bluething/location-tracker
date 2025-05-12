package io.github.bluething.playground.java.persistenceservice.repository;

import io.github.bluething.playground.java.persistenceservice.model.LocationEvent;

import java.time.Instant;
import java.util.List;

public interface LocationEventRepository {
    void save(LocationEvent event);
    List<LocationEvent> findByUserBetween(String userId, Instant from, Instant to);
    int countInArea(double minLat, double maxLat, double minLon, double maxLon, Instant since);
}
