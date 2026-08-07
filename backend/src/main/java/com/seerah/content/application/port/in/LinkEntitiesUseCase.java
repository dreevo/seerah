package com.seerah.content.application.port.in;

import com.seerah.shared.EntityType;
import com.seerah.shared.RelationshipType;

import java.util.UUID;

/** Create a typed, published edge between two entities (§12.5). */
public interface LinkEntitiesUseCase {

    UUID link(Command command);

    record Command(
            EntityType subjectType, UUID subjectId,
            RelationshipType relType,
            EntityType objectType, UUID objectId,
            double weight, String qualifier) {
    }
}
