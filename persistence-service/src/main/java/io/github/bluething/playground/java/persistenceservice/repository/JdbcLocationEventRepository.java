package io.github.bluething.playground.java.persistenceservice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bluething.playground.java.persistenceservice.model.LocationEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
class JdbcLocationEventRepository implements LocationEventRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcLocationEventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(LocationEvent event) {
        jdbcTemplate.update("INSERT INTO location_events (user_id, latitude, longitude, timestamp, labels) VALUES (?, ?, ?, ?, ?::jsonb)",
                event.userId(), event.latitude(), event.longitude(), Timestamp.from(event.timestamp()), toJson(event.labels()));
    }

    @Override
    public List<LocationEvent> findByUserBetween(String userId, Instant from, Instant to) {
        return jdbcTemplate.query(
                "SELECT * FROM location_events WHERE user_id = ? AND timestamp BETWEEN ? AND ?",
                rowMapper(), userId, Timestamp.from(from), Timestamp.from(to));
    }

    @Override
    public int countInArea(double minLat, double maxLat, double minLon, double maxLon, Instant since) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM location_events WHERE timestamp > ? AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?",
                Integer.class, Timestamp.from(since), minLat, maxLat, minLon, maxLon);
    }

    private String toJson(Map<String, String> labels) {
        try {
            return objectMapper.writeValueAsString(labels);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize labels", e);
        }
    }
    private RowMapper<LocationEvent> rowMapper() {
        return (rs, rowNum) -> {
            try {
                return new LocationEvent(
                        rs.getString("user_id"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getTimestamp("timestamp").toInstant(),
                        objectMapper.readValue(rs.getString("labels"), Map.class)
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
