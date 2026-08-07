package com.seerah.content.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The {@code content} module's published read contract for chronicles (§22.2).
 */
public interface ChronicleReadPort {

    /** Published chronicles, in display order, each with its published-event count. */
    List<ChronicleView> published();

    Optional<ChronicleView> bySlug(String slug);

    Optional<ChronicleView> byId(UUID id);

    /** Resolve a chronicle slug to its id (used by the ingestion seeder). */
    Optional<UUID> idBySlug(String slug);
}
