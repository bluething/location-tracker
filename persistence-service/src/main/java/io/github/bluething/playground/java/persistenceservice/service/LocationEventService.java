package io.github.bluething.playground.java.persistenceservice.service;

import io.github.bluething.playground.java.persistenceservice.model.LocationEvent;
import io.github.bluething.playground.java.persistenceservice.repository.LocationEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
class LocationEventService implements LocationEventUseCase {
    private final LocationEventRepository locationEventRepository;
    LocationEventService(LocationEventRepository locationEventRepository) {
        this.locationEventRepository = locationEventRepository;
    }

    @Override
    public List<LocationEvent> findByUser(String userId, Instant from, Instant to) {
        return locationEventRepository.findByUserBetween(userId, from, to);
    }

    @Override
    public int countEventsInArea(double minLat, double maxLat, double minLon, double maxLon, Instant since) {
        return locationEventRepository.countInArea(minLat, maxLat, minLon, maxLon, since);
    }
}
