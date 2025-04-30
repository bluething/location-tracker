package io.github.bluething.playground.java.ingestionservice.model;

import java.time.Instant;
import java.util.Map;

public record LocationEvent(String userId,
                           double latitude,
                           double longitude,
                           Instant timestamp,
                           Map<String, String> labels) {
}