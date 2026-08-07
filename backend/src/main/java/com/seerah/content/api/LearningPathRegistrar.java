package com.seerah.content.api;

import java.util.UUID;

/** Ingestion contract for curated learning paths. */
public interface LearningPathRegistrar {

    UUID createPath(String slug, String title, String blurb, String audience, Integer estMinutes);

    /** Append a step pointing at an event. */
    void addEventStep(UUID pathId, int ordinal, UUID eventId, String prompt);
}
