package com.seerah.shared;

/**
 * The vocabulary of typed edges between entities (§12.2 {@code relationship_type}).
 * Shared, because a relationship joins two entities that live in different
 * contexts (a person PARTICIPATED_IN an event; a verse was REVEALED_ABOUT one).
 */
public enum RelationshipType {
    PARTICIPATED_IN, LED, OPPOSED, ALLIED_WITH, PRECEDED, FOLLOWED,
    CAUSED, RESULTED_IN, OCCURRED_AT, TRAVELLED_TO, REVEALED_DURING,
    REVEALED_ABOUT, NARRATED_BY, MARRIED_TO, PARENT_OF, CHILD_OF,
    SIBLING_OF, FREED_BY, COMPANION_OF, TEACHER_OF, MENTIONED_IN,
    PART_OF, ILLUSTRATES, CONTRASTS_WITH, SUCCEEDED;

    /** Interpretive edges are inferences, not transmitted facts (§12.5, §13.3). */
    public boolean isInterpretiveByNature() {
        return this == CAUSED || this == RESULTED_IN || this == CONTRASTS_WITH || this == ILLUSTRATES;
    }
}
