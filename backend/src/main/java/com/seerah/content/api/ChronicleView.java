package com.seerah.content.api;

import java.util.UUID;

/**
 * The read-side projection of a chronicle — a connected chronology (the Seerah,
 * or a single prophet's story) that groups already-governed events. The platform
 * is a library of these (§ generalisation).
 */
public record ChronicleView(
        UUID id,
        String slug,
        String title,
        String titleAr,
        String subtitle,
        String blurb,
        String glyph,
        String kind,
        int ordinal,
        int eventCount) {
}
