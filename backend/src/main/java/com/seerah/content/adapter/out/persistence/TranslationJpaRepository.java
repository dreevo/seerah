package com.seerah.content.adapter.out.persistence;

import com.seerah.shared.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TranslationJpaRepository extends JpaRepository<TranslationJpaEntity, UUID> {

    Optional<TranslationJpaEntity> findByEntityTypeAndEntityIdAndFieldNameAndLocale(
            EntityType entityType, UUID entityId, String fieldName, String locale);
}
