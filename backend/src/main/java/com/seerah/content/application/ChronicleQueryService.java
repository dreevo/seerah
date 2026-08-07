package com.seerah.content.application;

import com.seerah.content.api.ChronicleReadPort;
import com.seerah.content.api.ChronicleView;
import com.seerah.content.application.port.out.ChronicleQueryPort;
import com.seerah.content.application.port.out.ChronicleQueryPort.ChronicleRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read side for chronicles: implements the published {@link ChronicleReadPort}. */
@Service
@Transactional(readOnly = true)
public class ChronicleQueryService implements ChronicleReadPort {

    private final ChronicleQueryPort queryPort;

    public ChronicleQueryService(ChronicleQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<ChronicleView> published() {
        return queryPort.allOrdered().stream().map(this::toView).toList();
    }

    @Override
    public Optional<ChronicleView> bySlug(String slug) {
        return queryPort.bySlug(slug).map(this::toView);
    }

    @Override
    public Optional<ChronicleView> byId(UUID id) {
        if (id == null) return Optional.empty();
        return queryPort.allOrdered().stream().filter(r -> r.id().equals(id)).findFirst().map(this::toView);
    }

    @Override
    public Optional<UUID> idBySlug(String slug) {
        return queryPort.bySlug(slug).map(ChronicleRow::id);
    }

    private ChronicleView toView(ChronicleRow r) {
        return new ChronicleView(r.id(), r.slug(), r.title(), r.titleAr(), r.subtitle(),
                r.blurb(), r.glyph(), r.kind(), r.ordinal(), (int) queryPort.publishedEventCount(r.id()));
    }
}
