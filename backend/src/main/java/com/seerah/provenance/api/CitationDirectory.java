package com.seerah.provenance.api;

import com.seerah.shared.EntityType;

import java.util.List;
import java.util.UUID;

/**
 * The {@code provenance} module's published contract. It answers the one question
 * other modules must ask before publishing anything: <em>is this claim supported,
 * and — where it is contested — are the competing positions on record?</em>
 * (§13.2, §13.4). Provenance is a core subdomain (§5.1.1); this narrow surface is
 * how the rest of the system leans on it without reaching inside.
 */
public interface CitationDirectory {

    /** Number of supporting citation links attached to the given target. */
    long countSupportingCitations(EntityType targetType, UUID targetId);

    /** Number of recorded scholarly positions for the given target. */
    int countScholarlyPositions(EntityType targetType, UUID targetId);

    /** The citations supporting a target, for display on its page. */
    List<CitationView> citationsFor(EntityType targetType, UUID targetId);

    /**
     * A citation as shown to a reader. {@code quote} is only populated when the
     * source is quotable; otherwise the work may be cited but not reproduced (§12.6).
     */
    record CitationView(String workTitle, String tier, String locator, boolean quotable, String quote, String grade) { }
}
