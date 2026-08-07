package com.seerah.content.api;

import com.seerah.shared.EntityType;

import java.util.List;
import java.util.UUID;

/**
 * Reads the neighbourhood of an entity — the edges that make the chronology a
 * connected graph rather than a list (§12.5). Returned in descending weight, so
 * the most relevant relationships lead the "related items" rail.
 */
public interface RelationshipReadPort {

    /** Outgoing edges: the entities this subject relates to. */
    List<RelatedEntity> neighboursOf(EntityType subjectType, UUID subjectId);

    /**
     * Incoming edges: the entities that relate <em>to</em> this object. Each result's
     * {@code objectType}/{@code objectId} carry the referencing subject — so for a
     * person this returns the events that name them.
     */
    List<RelatedEntity> referencesTo(EntityType objectType, UUID objectId);
}
