package com.seerah.content.adapter.out.persistence;

import com.seerah.shared.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RelationshipJpaRepository extends JpaRepository<RelationshipJpaEntity, UUID> {

    List<RelationshipJpaEntity> findBySubjectTypeAndSubjectIdOrderByWeightDesc(
            EntityType subjectType, UUID subjectId);

    List<RelationshipJpaEntity> findByObjectTypeAndObjectIdOrderByWeightDesc(
            EntityType objectType, UUID objectId);
}
