package com.seerah.content.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-side access to chronicle rows and their published-event counts (§8.6). */
public interface ChronicleQueryPort {

    record ChronicleRow(UUID id, String slug, String title, String titleAr, String subtitle,
                        String blurb, String glyph, String kind, int ordinal) {
    }

    List<ChronicleRow> allOrdered();

    Optional<ChronicleRow> bySlug(String slug);

    long publishedEventCount(UUID chronicleId);
}
