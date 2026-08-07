package com.seerah.scripture.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerseJpaRepository extends JpaRepository<VerseJpaEntity, UUID> {
    Optional<VerseJpaEntity> findBySurahNumberAndAyahNumber(Short surahNumber, Short ayahNumber);
}
