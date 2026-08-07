package com.seerah.content.application;

import com.seerah.content.api.RelatedEntity;
import com.seerah.content.api.RelationshipReadPort;
import com.seerah.content.application.port.in.LinkEntitiesUseCase;
import com.seerah.content.application.port.out.RelationshipStorePort;
import com.seerah.shared.EntityType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Creates and reads the typed edges between entities. Whether an edge is
 * interpretive is derived from its type (§12.5) — CAUSED and CONTRASTS_WITH are
 * inferences and are flagged so the UI can mark them and the review process can
 * apply the stricter citation rule (§13.3).
 */
@Service
@Transactional
public class RelationshipService implements LinkEntitiesUseCase, RelationshipReadPort {

    private final RelationshipStorePort store;

    public RelationshipService(RelationshipStorePort store) {
        this.store = store;
    }

    @Override
    public UUID link(Command c) {
        boolean interpretive = c.relType().isInterpretiveByNature();
        return store.save(c.subjectType(), c.subjectId(), c.relType(),
                c.objectType(), c.objectId(), c.weight(), interpretive, c.qualifier());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RelatedEntity> neighboursOf(EntityType subjectType, UUID subjectId) {
        return store.neighboursOf(subjectType, subjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RelatedEntity> referencesTo(EntityType objectType, UUID objectId) {
        return store.referencesTo(objectType, objectId);
    }
}
