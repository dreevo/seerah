package com.seerah.content.adapter.out.provenance;

import com.seerah.content.application.port.out.ProvenanceCheckPort;
import com.seerah.provenance.api.CitationDirectory;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Bridges the {@code content} module onto the {@code provenance} module — and does
 * so only through {@code provenance.api} (§6.8.2). This is the single seam where
 * content learns whether an event is adequately sourced; it never queries
 * provenance's tables.
 */
@Component
public class ProvenanceCheckAdapter implements ProvenanceCheckPort {

    private final CitationDirectory citationDirectory;

    public ProvenanceCheckAdapter(CitationDirectory citationDirectory) {
        this.citationDirectory = citationDirectory;
    }

    @Override
    public long countSupportingCitations(UUID eventId) {
        return citationDirectory.countSupportingCitations(EntityType.EVENT, eventId);
    }

    @Override
    public int countScholarlyPositions(UUID eventId) {
        return citationDirectory.countScholarlyPositions(EntityType.EVENT, eventId);
    }
}
