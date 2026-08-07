package com.seerah.content.application;

import com.seerah.content.api.ChronicleReadPort;
import com.seerah.content.api.EventDetailView;
import com.seerah.content.api.EventReadPort;
import com.seerah.content.api.EventSummaryView;
import com.seerah.content.application.port.out.EventQueryPort;
import com.seerah.content.application.port.out.EventTranslationPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The read side (§25.3 — read-only transactions). Implements the module's
 * published {@link EventReadPort}: it resolves the localised title and assembles
 * the {@link EventSummaryView} other modules consume.
 */
@Service
@Transactional(readOnly = true)
public class EventQueryService implements EventReadPort {

    private final EventQueryPort queryPort;
    private final EventTranslationPort translationPort;
    private final ChronicleReadPort chronicles;

    public EventQueryService(EventQueryPort queryPort, EventTranslationPort translationPort,
                             ChronicleReadPort chronicles) {
        this.queryPort = queryPort;
        this.translationPort = translationPort;
        this.chronicles = chronicles;
    }

    @Override
    public Optional<EventSummaryView> findById(UUID id, String locale) {
        return queryPort.byId(id).map(row -> toView(row, locale));
    }

    @Override
    public Optional<EventSummaryView> findBySlug(String slug, String locale) {
        return queryPort.bySlug(slug).map(row -> toView(row, locale));
    }

    @Override
    public List<EventSummaryView> publishedTimeline(String locale, String chronicleSlug) {
        UUID chronicleId = chronicleSlug == null ? null
                : chronicles.idBySlug(chronicleSlug).orElse(null);
        return queryPort.publishedOrdered(chronicleId).stream().map(row -> toView(row, locale)).toList();
    }

    @Override
    public Optional<EventDetailView> findDetailBySlug(String slug, String locale) {
        return queryPort.bySlug(slug).map(row -> toDetail(row, locale));
    }

    @Override
    public Optional<EventDetailView> findDetailById(UUID id, String locale) {
        return queryPort.byId(id).map(row -> toDetail(row, locale));
    }

    private EventSummaryView toView(EventQueryPort.EventRow row, String locale) {
        return new EventSummaryView(row.id(), row.slug(), title(row, locale), row.status(),
                row.certainty(), row.hijriYear(), row.gregYear(), row.major());
    }

    private EventDetailView toDetail(EventQueryPort.EventRow row, String locale) {
        var chronicle = chronicles.byId(row.chronicleId());
        return new EventDetailView(row.id(), row.slug(), title(row, locale),
                text(row.id(), "summary", locale), text(row.id(), "why", locale),
                row.certainty(), row.status(), row.hijriYear(), row.gregYear(), row.major(),
                chronicle.map(c -> c.slug()).orElse(null),
                chronicle.map(c -> c.title()).orElse(null));
    }

    private String title(EventQueryPort.EventRow row, String locale) {
        return translationPort.value(row.id(), "title", locale)
                .orElseGet(() -> translationPort.value(row.id(), "title", "en").orElse(row.slug()));
    }

    private String text(UUID id, String field, String locale) {
        return translationPort.value(id, field, locale)
                .orElseGet(() -> translationPort.value(id, field, "en").orElse(null));
    }
}
