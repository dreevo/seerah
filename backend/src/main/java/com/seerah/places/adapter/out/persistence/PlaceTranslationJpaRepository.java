package com.seerah.places.adapter.out.persistence;

import com.seerah.shared.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlaceTranslationJpaRepository extends JpaRepository<PlaceTranslationJpaEntity, UUID> {

    Optional<PlaceTranslationJpaEntity> findFirstByEntityTypeAndEntityIdAndFieldNameAndLocale(
            EntityType entityType, UUID entityId, String fieldName, String locale);
}
