package com.seerah.content.api;

import java.util.UUID;

/**
 * The read-side projection of an event, as other modules and the web layer see
 * it (§8.6 read models). A locale-resolved title is included: no read path can
 * return a useful event without a localised string (§11.2).
 */
public record EventSummaryView(
        UUID id,
        String slug,
        String title,
        String status,
        String certainty,
        Integer hijriYear,
        Integer gregYear,
        boolean major) {
}
