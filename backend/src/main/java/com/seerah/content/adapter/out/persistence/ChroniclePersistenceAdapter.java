package com.seerah.content.adapter.out.persistence;

import com.seerah.content.application.port.out.ChronicleQueryPort;
import com.seerah.shared.ContentStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound persistence adapter for chronicles. */
@Component
public class ChroniclePersistenceAdapter implements ChronicleQueryPort {

    private final ChronicleJpaRepository chronicles;
    private final EventJpaRepository events;

    public ChroniclePersistenceAdapter(ChronicleJpaRepository chronicles, EventJpaRepository events) {
        this.chronicles = chronicles;
        this.events = events;
    }

    @Override
    public List<ChronicleRow> allOrdered() {
        return chronicles.findAllByOrderByOrdinalAscTitleAsc().stream()
                .map(ChroniclePersistenceAdapter::toRow).toList();
    }

    @Override
    public Optional<ChronicleRow> bySlug(String slug) {
        return chronicles.findBySlug(slug).map(ChroniclePersistenceAdapter::toRow);
    }

    @Override
    public long publishedEventCount(UUID chronicleId) {
        return events.countByStatusAndChronicleId(ContentStatus.PUBLISHED, chronicleId);
    }

    private static ChronicleRow toRow(ChronicleJpaEntity c) {
        return new ChronicleRow(c.getId(), c.getSlug(), c.getTitle(), c.getTitleAr(),
                c.getSubtitle(), c.getBlurb(), c.getGlyph(), c.getKind(), c.getOrdinal());
    }
}
