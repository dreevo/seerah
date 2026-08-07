package com.seerah.provenance.adapter.out.persistence;

import com.seerah.shared.CitationRole;
import com.seerah.shared.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface CitationLinkJpaRepository extends JpaRepository<CitationLinkJpaEntity, UUID> {

    long countByTargetTypeAndTargetIdAndRoleIn(
            EntityType targetType, UUID targetId, Collection<CitationRole> roles);

    java.util.List<CitationLinkJpaEntity> findByTargetTypeAndTargetId(EntityType targetType, UUID targetId);
}
