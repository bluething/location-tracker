package io.github.bluething.playground.java.ingestionservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bluething.playground.java.ingestionservice.model.LocationEvent;
import io.github.bluething.playground.java.ingestionservice.model.RawLocation;
import io.github.bluething.playground.java.ingestionservice.producer.LocationEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationWebSocketHandlerTest {
    @Mock
    private LocationEventProducer producer;

    @Mock
    private WebSocketSession session;

    private ObjectMapper mapper;

    @InjectMocks
    private LocationWebSocketHandler handler;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
        handler = new LocationWebSocketHandler(mapper, producer);
    }

    @Test
    void handleTextMessage_validJson_callsProducer() throws Exception {
        RawLocation rawLocation = new RawLocation("user1",
                1.23,
                4.56);

        String payload = mapper.writeValueAsString(rawLocation);
        when(session.getId()).thenReturn("session1");

        handler.handleTextMessage(session, new TextMessage(payload));

        // Capture the LocationEvent passed to the producer
        ArgumentCaptor<LocationEvent> captor = ArgumentCaptor.forClass(LocationEvent.class);
        verify(producer).send(captor.capture());

        var sent = captor.getValue();
        assertEquals("user1", sent.userId());
        assertEquals(1.23, sent.latitude());
        assertEquals(4.56, sent.longitude());
        assertTrue(sent.labels().containsKey("sessionId"), "Labels should contain sessionId");
        assertEquals("session1", sent.labels().get("sessionId"), "sessionId label should match session ID");
        assertTrue(sent.labels().containsKey("source"), "Labels should contain source");

    }

    @Test
    void handleTextMessage_invalidJson_sendsError() throws Exception {
        TextMessage invalid = new TextMessage("not-json");
        when(session.getId()).thenReturn("session2");

        handler.handleTextMessage(session, invalid);

        // Capture the sent TextMessage and assert it starts with "ERROR"
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());
        TextMessage errorResponse = messageCaptor.getValue();

        String responsePayload = errorResponse.getPayload();
        // Ensure the response is an error message
        assertTrue(responsePayload.startsWith("ERROR"), "Error response should start with 'ERROR'");
        // Verify producer was never called
        verifyNoInteractions(producer);
    }
}