package com.seerah.content.application.port.in;

import com.seerah.shared.Certainty;

import java.util.UUID;

/** Create a new event in DRAFT, together with its title in the given locale. */
public interface CreateEventUseCase {

    UUID create(Command command);

    record Command(
            String slug,
            UUID chronicleId,
            String titleLocale,
            String title,
            int hijriYear,
            Integer gregorianYear,
            Certainty certainty,
            boolean major,
            int sortKey) {

        /** Convenience for callers with no chronicle (tests / legacy). */
        public Command(String slug, String titleLocale, String title, int hijriYear,
                       Integer gregorianYear, Certainty certainty, boolean major, int sortKey) {
            this(slug, null, titleLocale, title, hijriYear, gregorianYear, certainty, major, sortKey);
        }
    }
}
