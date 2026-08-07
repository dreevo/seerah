package com.seerah.media.adapter.out.persistence;

import com.seerah.shared.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Top-level repository interfaces (Spring Data does not bind nested ones). */
interface MediaAssetJpaRepository extends JpaRepository<MediaAssetJpaEntity, UUID> {
}

interface MediaLinkJpaRepository extends JpaRepository<MediaLinkJpaEntity, UUID> {
    List<MediaLinkJpaEntity> findByTargetTypeAndTargetIdOrderByOrdinalAsc(EntityType targetType, UUID targetId);
}
