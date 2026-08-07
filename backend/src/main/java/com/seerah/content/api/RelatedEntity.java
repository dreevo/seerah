package com.seerah.content.api;

import com.seerah.shared.EntityType;
import com.seerah.shared.RelationshipType;

import java.util.UUID;

/** One edge out of an entity: what it relates to, how, and how strongly (§12.5). */
public record RelatedEntity(
        RelationshipType relType,
        EntityType objectType,
        UUID objectId,
        double weight,
        boolean interpretive,
        String qualifier) {
}
