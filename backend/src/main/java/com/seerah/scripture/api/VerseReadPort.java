package com.seerah.scripture.api;

import java.util.Optional;
import java.util.UUID;

/** Published read contract of the {@code scripture} module. */
public interface VerseReadPort {

    Optional<VerseView> findById(UUID verseId, String locale);

    Optional<VerseView> findByRef(int surahNumber, int ayahNumber, String locale);
}
