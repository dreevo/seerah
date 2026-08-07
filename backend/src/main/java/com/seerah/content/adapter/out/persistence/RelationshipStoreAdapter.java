package com.seerah.content.adapter.out.persistence;

import com.seerah.content.api.RelatedEntity;
import com.seerah.content.application.port.out.RelationshipStorePort;
import com.seerah.shared.EntityType;
import com.seerah.shared.RelationshipType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RelationshipStoreAdapter implements RelationshipStorePort {

    private final RelationshipJpaRepository repository;

    public RelationshipStoreAdapter(RelationshipJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public UUID save(EntityType subjectType, UUID subjectId, RelationshipType relType,
                     EntityType objectType, UUID objectId, double weight, boolean interpretive,
                     String qualifier) {
        return repository.save(RelationshipJpaEntity.create(
                subjectType, subjectId, relType, objectType, objectId, weight, interpretive, qualifier)).getId();
    }

    @Override
    public List<RelatedEntity> neighboursOf(EntityType subjectType, UUID subjectId) {
        return repository.findBySubjectTypeAndSubjectIdOrderByWeightDesc(subjectType, subjectId).stream()
                .map(r -> new RelatedEntity(r.getRelType(), r.getObjectType(), r.getObjectId(),
                        r.getWeight().doubleValue(), r.isInterpretive(), r.getQualifier()))
                .toList();
    }

    @Override
    public List<RelatedEntity> referencesTo(EntityType objectType, UUID objectId) {
        return repository.findByObjectTypeAndObjectIdOrderByWeightDesc(objectType, objectId).stream()
                .map(r -> new RelatedEntity(r.getRelType(), r.getSubjectType(), r.getSubjectId(),
                        r.getWeight().doubleValue(), r.isInterpretive(), r.getQualifier()))
                .toList();
    }
}
