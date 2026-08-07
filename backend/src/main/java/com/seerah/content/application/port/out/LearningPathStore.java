package com.seerah.content.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound store for learning paths and their steps. */
public interface LearningPathStore {

    UUID createPath(String slug, String audience, Integer estMinutes);

    void addEventStep(UUID pathId, int ordinal, UUID eventId, String prompt);

    record PathRow(UUID id, String slug, String audience, Integer estMinutes, int stepCount) { }

    record StepRow(int ordinal, UUID eventId, String prompt) { }

    List<PathRow> publishedPaths();

    Optional<PathRow> pathBySlug(String slug);

    List<StepRow> stepsOf(UUID pathId);
}
