package com.seerah.provenance.api;

import com.seerah.shared.CitationRole;
import com.seerah.shared.EntityType;
import com.seerah.shared.HadithGrade;
import com.seerah.shared.SourceTier;

import java.util.UUID;

/**
 * The write side of the {@code provenance} contract. Editors (and, in tests,
 * fixtures) register works, attach citations to claims, and record scholarly
 * positions through this narrow interface.
 */
public interface CitationRegistrar {

    UUID registerSource(RegisterSource command);

    /** Create a citation into a source and link it to a target claim in one step. */
    UUID addCitation(AddCitation command);

    UUID addScholarlyPosition(AddScholarlyPosition command);

    record RegisterSource(String slug, String workTitle, String author,
                          SourceTier tier, boolean quotable) {
    }

    record AddCitation(UUID sourceId, String locator, String locatorKind, String quote,
                       HadithGrade grade, EntityType targetType, UUID targetId,
                       CitationRole role, String fieldName) {
    }

    record AddScholarlyPosition(EntityType targetType, UUID targetId, String positionKey,
                                String heldBy, String summary, UUID citationId, int ordinal) {
    }
}
