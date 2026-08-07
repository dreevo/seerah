package com.seerah.content.api;

import java.util.UUID;

/** The full localised detail of an event, assembled from row + translations (§11.2). */
public record EventDetailView(
        UUID id,
        String slug,
        String title,
        String summary,
        String why,
        String certainty,
        String status,
        Integer hijriYear,
        Integer gregYear,
        boolean major,
        String chronicleSlug,
        String chronicleTitle) {
}
