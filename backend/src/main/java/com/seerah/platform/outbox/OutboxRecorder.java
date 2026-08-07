package com.seerah.platform.outbox;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The one supported way to announce a domain event (§25.5, §5.11 — "make the
 * correct thing easy and the incorrect thing impossible"). Application services
 * call this inside their transaction; there is deliberately no path to publish
 * to a broker directly.
 */
@Component
public class OutboxRecorder {

    private final OutboxJpaRepository repository;

    public OutboxRecorder(OutboxJpaRepository repository) {
        this.repository = repository;
    }

    /**
     * Record an event in the outbox. Must be invoked within the caller's active
     * transaction so the aggregate change and this row commit together.
     *
     * @param aggregateType e.g. {@code "event"}
     * @param aggregateId   the aggregate's id
     * @param eventType     e.g. {@code "content.event.published.v1"}
     * @param jsonPayload   a JSON document describing the change
     */
    public void record(String aggregateType, UUID aggregateId, String eventType, String jsonPayload) {
        repository.save(new OutboxRecord(aggregateType, aggregateId, eventType, jsonPayload, currentTraceId()));
    }

    private String currentTraceId() {
        // Wired to the MDC correlation id in the observability phase (§27.5).
        return null;
    }
}
