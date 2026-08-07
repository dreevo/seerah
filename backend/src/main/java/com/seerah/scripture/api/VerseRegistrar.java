package com.seerah.scripture.api;

import com.seerah.scripture.domain.RevelationPlace;

import java.util.List;
import java.util.UUID;

/**
 * Ingestion contract for scripture reference data. Surah and verse are seeded
 * from a vetted upstream set (§12.7); they are not editorial content and carry
 * no review workflow.
 */
public interface VerseRegistrar {

    void upsertSurah(int number, String nameAr, String nameTranslit, String nameEn,
                     RevelationPlace place, int revelationOrder, int ayahCount);

    UUID upsertVerse(int surahNumber, int ayahNumber, String textUthmani, String textSimple);

    void upsertTranslation(UUID verseId, String locale, String translator, String text);

    // --- bulk reference load (the full Qur'an, §12.7) -----------------------

    /** True if the reference corpus is already present (idempotency guard). */
    boolean isReferenceLoaded();

    /** Bulk-load the whole corpus in one batched pass. */
    void loadReference(String translator, List<SurahRow> surahs, List<VerseRow> verses);

    record SurahRow(int number, String nameAr, String translit, RevelationPlace place, int ayahCount) { }

    record VerseRow(int surah, int ayah, String uthmani, String translation) { }
}
