package com.seerah.content.api;

import java.util.List;

/** Read shapes for learning paths (the "Guided Journeys"). */
public final class LearningPathViews {

    private LearningPathViews() { }

    public record PathSummary(String slug, String title, String blurb,
                              String audience, Integer estMinutes, int stepCount) { }

    public record PathStep(int ordinal, String eventSlug, String eventTitle, String prompt) { }

    public record PathDetail(String slug, String title, String blurb,
                             String audience, Integer estMinutes, List<PathStep> steps) { }
}
