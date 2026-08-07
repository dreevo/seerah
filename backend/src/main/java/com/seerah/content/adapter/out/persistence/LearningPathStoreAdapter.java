package com.seerah.content.adapter.out.persistence;

import com.seerah.content.application.port.out.LearningPathStore;
import com.seerah.shared.ContentStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LearningPathStoreAdapter implements LearningPathStore {

    private final LearningPathJpaRepository paths;
    private final LearningPathStepJpaRepository steps;

    public LearningPathStoreAdapter(LearningPathJpaRepository paths, LearningPathStepJpaRepository steps) {
        this.paths = paths;
        this.steps = steps;
    }

    @Override
    public UUID createPath(String slug, String audience, Integer estMinutes) {
        return paths.save(LearningPathJpaEntity.create(slug, audience, estMinutes)).getId();
    }

    @Override
    public void addEventStep(UUID pathId, int ordinal, UUID eventId, String prompt) {
        steps.save(LearningPathStepJpaEntity.event(pathId, ordinal, eventId, prompt));
    }

    @Override
    public List<PathRow> publishedPaths() {
        return paths.findByStatusOrderBySlugAsc(ContentStatus.PUBLISHED).stream()
                .map(this::toRow).toList();
    }

    @Override
    public Optional<PathRow> pathBySlug(String slug) {
        return paths.findBySlug(slug).map(this::toRow);
    }

    @Override
    public List<StepRow> stepsOf(UUID pathId) {
        return steps.findByPathIdOrderByOrdinalAsc(pathId).stream()
                .map(s -> new StepRow(s.getOrdinal(), s.getTargetId(), s.getPrompt()))
                .toList();
    }

    private PathRow toRow(LearningPathJpaEntity p) {
        int count = (int) steps.countByPathId(p.getId());
        Integer est = p.getEstMinutes() == null ? null : p.getEstMinutes().intValue();
        return new PathRow(p.getId(), p.getSlug(), p.getAudience(), est, count);
    }
}
