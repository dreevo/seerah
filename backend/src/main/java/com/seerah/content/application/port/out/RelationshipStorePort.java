package com.seerah.content.application.port.out;

import com.seerah.content.api.RelatedEntity;
import com.seerah.shared.EntityType;
import com.seerah.shared.RelationshipType;

import java.util.List;
import java.util.UUID;

/** Outbound store for relationship edges. */
public interface RelationshipStorePort {

    UUID save(EntityType subjectType, UUID subjectId, RelationshipType relType,
              EntityType objectType, UUID objectId, double weight, boolean interpretive,
              String qualifier);

    List<RelatedEntity> neighboursOf(EntityType subjectType, UUID subjectId);

    List<RelatedEntity> referencesTo(EntityType objectType, UUID objectId);
}
