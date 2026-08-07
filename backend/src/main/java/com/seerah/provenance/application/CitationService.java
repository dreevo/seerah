package com.seerah.provenance.application;

import com.seerah.provenance.api.CitationDirectory;
import com.seerah.provenance.api.CitationRegistrar;
import com.seerah.provenance.application.port.out.ProvenanceStore;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The provenance application service. Implements both published ports — the
 * read-side {@link CitationDirectory} and the write-side {@link CitationRegistrar} —
 * over a single {@link ProvenanceStore} out-port.
 */
@Service
@Transactional
public class CitationService implements CitationRegistrar, CitationDirectory {

    private final ProvenanceStore store;

    public CitationService(ProvenanceStore store) {
        this.store = store;
    }

    @Override
    public UUID registerSource(RegisterSource c) {
        return store.saveSource(c.slug(), c.workTitle(), c.author(), c.tier(), c.quotable());
    }

    @Override
    public UUID addCitation(AddCitation c) {
        UUID citationId = store.saveCitation(c.sourceId(), c.locator(), c.locatorKind(), c.quote(), c.grade());
        store.saveLink(citationId, c.targetType(), c.targetId(), c.role(), c.fieldName());
        return citationId;
    }

    @Override
    public UUID addScholarlyPosition(AddScholarlyPosition c) {
        return store.savePosition(c.targetType(), c.targetId(), c.positionKey(), c.heldBy(),
                c.summary(), c.citationId(), c.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public long countSupportingCitations(EntityType targetType, UUID targetId) {
        return store.countSupportingLinks(targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countScholarlyPositions(EntityType targetType, UUID targetId) {
        return store.countPositions(targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitationView> citationsFor(EntityType targetType, UUID targetId) {
        return store.listCitationsFor(targetType, targetId).stream()
                .map(d -> new CitationView(d.workTitle(), d.tier(), d.locator(), d.quotable(), d.quote(), d.grade()))
                .toList();
    }
}
