package com.seerah.people.api;

import java.util.UUID;

/** Read projection of a person, as other modules and the web layer see them. */
public record PersonSummaryView(
        UUID id,
        String slug,
        String name,
        String nameArabic,
        String role,
        String status,
        Integer deathYearAh) {
}
