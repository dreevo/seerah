package com.seerah.search.adapter.in.scheduler;

import com.seerah.content.api.EventReadPort;
import com.seerah.people.api.PersonReadPort;
import com.seerah.platform.outbox.OutboxJpaRepository;
import com.seerah.platform.outbox.OutboxRecord;
import com.seerah.search.application.port.out.SearchIndexWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The transactional-outbox relay (§25.5). In Phase 1 it is exactly what the record
 * calls it — "a housekeeping job": a poller that reads the outbox and projects
 * published events and people into the OpenSearch index, then marks each row done.
 * In the CDC phase, Debezium tails the same table and this poller is retired; the
 * outbox contract does not change (see {@code resources/debezium/}).
 *
 * <p>Active only when {@code search.engine=opensearch}; the default Postgres engine
 * searches the tables directly and needs no projection.
 */
@Component
@ConditionalOnProperty(name = "search.engine", havingValue = "opensearch")
public class SearchProjectionRelay {

    private final OutboxJpaRepository outbox;
    private final EventReadPort events;
    private final PersonReadPort people;
    private final SearchIndexWriter writer;
    private volatile boolean indexReady = false;

    public SearchProjectionRelay(OutboxJpaRepository outbox, EventReadPort events,
                                 PersonReadPort people, SearchIndexWriter writer) {
        this.outbox = outbox;
        this.events = events;
        this.people = people;
        this.writer = writer;
    }

    @Scheduled(fixedDelayString = "${search.relay.interval-ms:2000}")
    public void tick() {
        try {
            drainOnce();
        } catch (RuntimeException ex) {
            // A relay failure must never crash the app; the rows stay unprocessed
            // and are retried on the next tick.
        }
    }

    /** Process one batch of outbox rows. Public so tests can drive it deterministically. */
    @Transactional
    public int drainOnce() {
        if (!indexReady) {
            writer.ensureIndex();
            indexReady = true;
        }
        List<OutboxRecord> batch = outbox.findTop100ByPublishedAtIsNullOrderByIdAsc();
        if (batch.isEmpty()) {
            return 0;
        }
        for (OutboxRecord r : batch) {
            project(r);
            r.markProcessed();
        }
        outbox.saveAll(batch);
        writer.refresh();
        return batch.size();
    }

    private void project(OutboxRecord r) {
        switch (r.getEventType()) {
            case "content.event.published.v1" -> events.findDetailById(r.getAggregateId(), "en")
                    .ifPresent(e -> writer.index("EVENT", e.id(), e.slug(),
                            e.title() + " " + (e.summary() == null ? "" : e.summary())));
            case "people.person.published.v1" -> people.findById(r.getAggregateId(), "en")
                    .ifPresent(p -> writer.index("PERSON", p.id(), p.slug(),
                            p.name() + " " + (p.nameArabic() == null ? "" : p.nameArabic())));
            default -> { /* not a searchable publication; just mark processed */ }
        }
    }
}
