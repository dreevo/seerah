package com.seerah.content.application.port.out;

import java.util.UUID;

/**
 * The content module's view onto provenance, needed to enforce the publish
 * invariants. The implementing adapter ({@code content.adapter.out.provenance})
 * delegates to {@code provenance.api} — content never touches provenance's
 * internals (§6.8.2).
 */
public interface ProvenanceCheckPort {

    /** Count of supporting citation links attached to this event (§13.2). */
    long countSupportingCitations(UUID eventId);

    /** Count of recorded scholarly positions for this event (§13.4). */
    int countScholarlyPositions(UUID eventId);
}
