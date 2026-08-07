package com.seerah.scripture.application;

import com.seerah.scripture.api.VerseReadPort;
import com.seerah.scripture.api.VerseRegistrar;
import com.seerah.scripture.api.VerseView;
import com.seerah.scripture.application.port.out.ScriptureStore;
import com.seerah.scripture.domain.RevelationPlace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Application service for scripture: reference ingestion and verse reads. */
@Service
@Transactional
public class VerseService implements VerseReadPort, VerseRegistrar {

    private final ScriptureStore store;

    public VerseService(ScriptureStore store) {
        this.store = store;
    }

    @Override
    public void upsertSurah(int number, String nameAr, String nameTranslit, String nameEn,
                            RevelationPlace place, int revelationOrder, int ayahCount) {
        store.upsertSurah(number, nameAr, nameTranslit, nameEn, place, revelationOrder, ayahCount);
    }

    @Override
    public UUID upsertVerse(int surahNumber, int ayahNumber, String textUthmani, String textSimple) {
        return store.upsertVerse(surahNumber, ayahNumber, textUthmani, textSimple);
    }

    @Override
    public void upsertTranslation(UUID verseId, String locale, String translator, String text) {
        store.upsertTranslation(verseId, locale, translator, text);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReferenceLoaded() {
        return store.hasSurahs();
    }

    @Override
    public void loadReference(String translator, java.util.List<SurahRow> surahs, java.util.List<VerseRow> verses) {
        store.bulkLoad(translator, surahs, verses);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VerseView> findById(UUID verseId, String locale) {
        return store.findById(verseId, locale).map(VerseService::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VerseView> findByRef(int surahNumber, int ayahNumber, String locale) {
        return store.findByRef(surahNumber, ayahNumber, locale).map(VerseService::toView);
    }

    private static VerseView toView(ScriptureStore.VerseData d) {
        return new VerseView(d.id(), d.surahNumber(), d.ayahNumber(),
                d.surahNumber() + ":" + d.ayahNumber(), d.surahNameEn(), d.surahNameAr(),
                d.textUthmani(), d.translationText(), d.translator());
    }
}
