package com.seerah.content.application;

import com.seerah.content.api.EventReadPort;
import com.seerah.content.api.LearningPathReadPort;
import com.seerah.content.api.LearningPathRegistrar;
import com.seerah.content.api.LearningPathViews.PathDetail;
import com.seerah.content.api.LearningPathViews.PathStep;
import com.seerah.content.api.LearningPathViews.PathSummary;
import com.seerah.content.application.port.out.ContentTextPort;
import com.seerah.content.application.port.out.LearningPathStore;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Curated learning paths — the product's "Guided Journeys". Titles and blurbs are
 * stored in translation (§11.2); each step resolves to a published event's slug
 * and localised title so the reader can walk the sequence.
 */
@Service
@Transactional
public class LearningPathService implements LearningPathReadPort, LearningPathRegistrar {

    private final LearningPathStore store;
    private final ContentTextPort text;
    private final EventReadPort events;

    public LearningPathService(LearningPathStore store, ContentTextPort text, EventReadPort events) {
        this.store = store;
        this.text = text;
        this.events = events;
    }

    @Override
    public UUID createPath(String slug, String title, String blurb, String audience, Integer estMinutes) {
        UUID id = store.createPath(slug, audience, estMinutes);
        text.putText(EntityType.LEARNING_PATH, id, "title", "en", title);
        if (blurb != null) text.putText(EntityType.LEARNING_PATH, id, "blurb", "en", blurb);
        return id;
    }

    @Override
    public void addEventStep(UUID pathId, int ordinal, UUID eventId, String prompt) {
        store.addEventStep(pathId, ordinal, eventId, prompt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathSummary> publishedPaths(String locale) {
        return store.publishedPaths().stream()
                .map(p -> new PathSummary(p.slug(),
                        title(p.id(), locale, p.slug()), blurb(p.id(), locale),
                        p.audience(), p.estMinutes(), p.stepCount()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PathDetail> pathBySlug(String slug, String locale) {
        return store.pathBySlug(slug).map(p -> {
            List<PathStep> steps = new ArrayList<>();
            for (LearningPathStore.StepRow s : store.stepsOf(p.id())) {
                var e = events.findById(s.eventId(), locale).orElse(null);
                if (e != null) {
                    steps.add(new PathStep(s.ordinal(), e.slug(), e.title(), s.prompt()));
                }
            }
            return new PathDetail(p.slug(), title(p.id(), locale, p.slug()), blurb(p.id(), locale),
                    p.audience(), p.estMinutes(), steps);
        });
    }

    private String title(UUID id, String locale, String fallback) {
        return text.text(EntityType.LEARNING_PATH, id, "title", locale)
                .or(() -> text.text(EntityType.LEARNING_PATH, id, "title", "en"))
                .orElse(fallback);
    }

    private String blurb(UUID id, String locale) {
        return text.text(EntityType.LEARNING_PATH, id, "blurb", locale)
                .or(() -> text.text(EntityType.LEARNING_PATH, id, "blurb", "en"))
                .orElse(null);
    }
}
