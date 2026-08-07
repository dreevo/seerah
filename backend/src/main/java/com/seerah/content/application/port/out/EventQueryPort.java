package com.seerah.content.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-side access to event rows for building views. Kept separate from
 * {@link LoadEventPort} (which returns the mutable aggregate) because reads and
 * writes have genuinely different shapes (§8.6).
 */
public interface EventQueryPort {

    record EventRow(UUID id, String slug, String status, String certainty,
                    Integer hijriYear, Integer gregYear, boolean major, UUID chronicleId) {
    }

    Optional<EventRow> byId(UUID id);

    Optional<EventRow> bySlug(String slug);

    /** Published events in timeline order (sort_key, greg_start, id) across all chronicles. */
    default List<EventRow> publishedOrdered() {
        return publishedOrdered(null);
    }

    /** Published events in timeline order, optionally scoped to one chronicle (null = all). */
    List<EventRow> publishedOrdered(UUID chronicleId);
}
