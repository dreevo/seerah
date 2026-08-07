package com.seerah.content.adapter.out.persistence;

import com.seerah.content.application.port.out.EventQueryPort;
import com.seerah.content.application.port.out.LoadEventPort;
import com.seerah.content.application.port.out.SaveEventPort;
import com.seerah.content.domain.Event;
import com.seerah.content.domain.EventId;
import com.seerah.shared.ContentStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The outbound persistence adapter for the {@code content} module. Implements the
 * write ports ({@link SaveEventPort}, {@link LoadEventPort}) and the read port
 * ({@link EventQueryPort}); the application depends only on those interfaces and
 * never on JPA (§23.1 — the dependency rule).
 */
@Component
public class EventPersistenceAdapter implements SaveEventPort, LoadEventPort, EventQueryPort {

    private final EventJpaRepository repository;

    public EventPersistenceAdapter(EventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Event event) {
        EventJpaEntity entity = repository.findById(event.id().value()).orElseGet(EventJpaEntity::new);
        EventMapper.applyToEntity(event, entity);
        // Flush now so the publish trigger (ct_event_fresh_approval, §13.6) fires
        // synchronously and its error surfaces at the call site, not at commit.
        repository.saveAndFlush(entity);
    }

    @Override
    public Optional<Event> load(EventId id) {
        return repository.findById(id.value()).map(EventMapper::toDomain);
    }

    @Override
    public Optional<EventRow> byId(UUID id) {
        return repository.findById(id).map(EventPersistenceAdapter::toRow);
    }

    @Override
    public Optional<EventRow> bySlug(String slug) {
        return repository.findBySlug(slug).map(EventPersistenceAdapter::toRow);
    }

    @Override
    public List<EventRow> publishedOrdered(UUID chronicleId) {
        var rows = chronicleId == null
                ? repository.findByStatusOrderBySortKeyAscGregStartAscIdAsc(ContentStatus.PUBLISHED)
                : repository.findByStatusAndChronicleIdOrderBySortKeyAscGregStartAscIdAsc(
                        ContentStatus.PUBLISHED, chronicleId);
        return rows.stream().map(EventPersistenceAdapter::toRow).toList();
    }

    private static EventRow toRow(EventJpaEntity e) {
        Integer gregYear = e.getGregStart() == null ? null : e.getGregStart().getYear();
        return new EventRow(e.getId(), e.getSlug(), e.getStatus().name(),
                e.getCertainty().name(), e.getHijriYear(), gregYear, e.isMajor(), e.getChronicleId());
    }
}
