package com.seerah.scripture.application.port.out;

import com.seerah.scripture.domain.RevelationPlace;

import java.util.Optional;
import java.util.UUID;

/** Outbound store for scripture reference data. */
public interface ScriptureStore {

    void upsertSurah(int number, String nameAr, String nameTranslit, String nameEn,
                     RevelationPlace place, int revelationOrder, int ayahCount);

    UUID upsertVerse(int surahNumber, int ayahNumber, String textUthmani, String textSimple);

    void upsertTranslation(UUID verseId, String locale, String translator, String text);

    record VerseData(UUID id, int surahNumber, int ayahNumber, String surahNameEn, String surahNameAr,
                     String textUthmani, String translationText, String translator) { }

    Optional<VerseData> findById(UUID verseId, String locale);

    Optional<VerseData> findByRef(int surahNumber, int ayahNumber, String locale);

    // --- bulk reference load ------------------------------------------------

    boolean hasSurahs();

    void bulkLoad(String translator,
                  java.util.List<com.seerah.scripture.api.VerseRegistrar.SurahRow> surahs,
                  java.util.List<com.seerah.scripture.api.VerseRegistrar.VerseRow> verses);
}
