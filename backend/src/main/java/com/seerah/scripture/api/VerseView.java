package com.seerah.scripture.api;

import java.util.UUID;

/**
 * A verse as presented to a reader: verbatim Uthmani text, a named translation of
 * meaning (never labelled "the Quran"), and its reference. The Arabic is served
 * exactly as stored — never transformed (§12.7).
 */
public record VerseView(
        UUID id,
        int surahNumber,
        int ayahNumber,
        String reference,
        String surahNameEn,
        String surahNameAr,
        String textUthmani,
        String translationText,
        String translator) {
}
