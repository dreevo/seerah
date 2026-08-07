package com.seerah.content.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LearningPathStepJpaRepository extends JpaRepository<LearningPathStepJpaEntity, UUID> {

    List<LearningPathStepJpaEntity> findByPathIdOrderByOrdinalAsc(UUID pathId);

    long countByPathId(UUID pathId);
}
