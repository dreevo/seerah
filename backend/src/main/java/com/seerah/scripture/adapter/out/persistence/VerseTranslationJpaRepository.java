package com.seerah.scripture.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerseTranslationJpaRepository extends JpaRepository<VerseTranslationJpaEntity, UUID> {
    Optional<VerseTranslationJpaEntity> findFirstByVerseIdAndLocale(UUID verseId, String locale);
    Optional<VerseTranslationJpaEntity> findFirstByVerseId(UUID verseId);
}
