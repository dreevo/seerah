package com.seerah.provenance.adapter.out.persistence;

import com.seerah.shared.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScholarlyPositionJpaRepository extends JpaRepository<ScholarlyPositionJpaEntity, UUID> {

    long countByTargetTypeAndTargetId(EntityType targetType, UUID targetId);
}
