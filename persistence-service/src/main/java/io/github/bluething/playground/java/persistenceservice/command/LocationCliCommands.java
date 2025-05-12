package io.github.bluething.playground.java.persistenceservice.command;

import io.github.bluething.playground.java.persistenceservice.model.LocationEvent;
import io.github.bluething.playground.java.persistenceservice.service.LocationEventUseCase;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.time.Instant;
import java.util.List;

@ShellComponent
class LocationCliCommands {
    private final LocationEventUseCase locationEventUseCase;

    LocationCliCommands(LocationEventUseCase locationEventUseCase) {
        this.locationEventUseCase = locationEventUseCase;
    }

    @ShellMethod("Query location events by user and time range")
    List<LocationEvent> queryUserEvents(String userId, String from, String to) {
        return locationEventUseCase.findByUser(userId, Instant.parse(from), Instant.parse(to));
    }

    @ShellMethod("Count events in area since timestamp")
    int countEvents(double minLat, double maxLat, double minLon, double maxLon, String since) {
        return locationEventUseCase.countEventsInArea(minLat, maxLat, minLon, maxLon, Instant.parse(since));
    }
}
