package com.seerah.content.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The {@code content} module's published read contract (§22.2 — the {@code api}
 * sub-package convention). Other modules depend on this interface and nothing
 * else inside {@code content}; the ArchUnit suite enforces it.
 */
public interface EventReadPort {

    Optional<EventSummaryView> findById(UUID id, String locale);

    Optional<EventSummaryView> findBySlug(String slug, String locale);

    /** Full localised detail for one event. */
    Optional<EventDetailView> findDetailBySlug(String slug, String locale);

    Optional<EventDetailView> findDetailById(UUID id, String locale);

    /** Published events in timeline order (sort_key, greg_start, id), across all chronicles. */
    default List<EventSummaryView> publishedTimeline(String locale) {
        return publishedTimeline(locale, null);
    }

    /** Published events in timeline order, optionally scoped to one chronicle by slug (null = all). */
    List<EventSummaryView> publishedTimeline(String locale, String chronicleSlug);
}
