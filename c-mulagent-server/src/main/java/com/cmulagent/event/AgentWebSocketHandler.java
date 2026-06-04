package com.cmulagent.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public class AgentWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);

    private final Sinks.Many<AgentEvent> eventSink = Sinks.many().multicast().onBackpressureBuffer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<String> eventStream = eventSink.asFlux()
                .map(event -> {
                    try {
                        return objectMapper.writeValueAsString(event);
                    } catch (Exception e) {
                        log.error("Failed to serialize event", e);
                        return "{}";
                    }
                });

        return session.send(
                eventStream.map(session::textMessage)
        ).and(session.receive()
                .map(org.springframework.web.reactive.socket.WebSocketMessage::getPayloadAsText)
                .doOnNext(msg -> log.debug("Received WS message: {}", msg))
                .then()
        );
    }

    public void publishEvent(AgentEvent event) {
        eventSink.tryEmitNext(event);
    }
}