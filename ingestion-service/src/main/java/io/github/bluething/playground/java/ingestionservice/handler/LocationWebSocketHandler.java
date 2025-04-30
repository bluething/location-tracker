package io.github.bluething.playground.java.ingestionservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bluething.playground.java.ingestionservice.model.LocationEvent;
import io.github.bluething.playground.java.ingestionservice.model.RawLocation;
import io.github.bluething.playground.java.ingestionservice.producer.LocationEventProducer;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;

@Service
public class LocationWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(LocationWebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final LocationEventProducer locationEventProducer;

    public LocationWebSocketHandler(ObjectMapper objectMapper, LocationEventProducer locationEventProducer) {
        this.objectMapper = objectMapper;
        this.locationEventProducer = locationEventProducer;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("Connection closed: {} -> {}", session.getId(), status.getReason());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("Connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            RawLocation rawLocation = objectMapper.readValue(message.getPayload(), RawLocation.class);
            LocationEvent locationEvent = enrich(rawLocation, session);
            locationEventProducer.send(locationEvent);
        } catch (Exception e) {
            logger.warn("Failed to process message from session {}: payload='{}'",
                    session.getId(), message.getPayload(), e);
            try {
                session.sendMessage(new TextMessage("ERROR: invalid payload"));
            } catch (IOException ex) {
                logger.error("Failed to send error message to session {}", session.getId(), ex);
            }
        }
    }

    private LocationEvent enrich(RawLocation rawLocation, WebSocketSession session) {
        Map<String, String> labels = Map.of("source",    "websocket",
                "sessionId", session.getId());
        return new LocationEvent(rawLocation.userId(),
                rawLocation.latitude(),
                rawLocation.longitude(),
                Instant.now(),
                labels);
    }
}
