package com.seerah.scripture.adapter.out.persistence;

import com.seerah.scripture.api.VerseRegistrar;
import com.seerah.scripture.application.port.out.ScriptureStore;
import com.seerah.scripture.domain.RevelationPlace;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ScriptureStoreAdapter implements ScriptureStore {

    private final SurahJpaRepository surahs;
    private final VerseJpaRepository verses;
    private final VerseTranslationJpaRepository translations;
    private final JdbcTemplate jdbc;

    public ScriptureStoreAdapter(SurahJpaRepository surahs, VerseJpaRepository verses,
                                 VerseTranslationJpaRepository translations, JdbcTemplate jdbc) {
        this.surahs = surahs;
        this.verses = verses;
        this.translations = translations;
        this.jdbc = jdbc;
    }

    @Override
    public boolean hasSurahs() {
        return surahs.count() > 0;
    }

    @Override
    public void bulkLoad(String translator, List<VerseRegistrar.SurahRow> surahRows,
                         List<VerseRegistrar.VerseRow> verseRows) {
        jdbc.batchUpdate("""
                INSERT INTO surah (number, name_ar, name_translit, name_en, revelation_place,
                                   revelation_order, ayah_count, has_bismillah)
                VALUES (?, ?, ?, ?, CAST(? AS revelation_place), ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                VerseRegistrar.SurahRow s = surahRows.get(i);
                ps.setShort(1, (short) s.number());
                ps.setString(2, s.nameAr());
                ps.setString(3, s.translit());
                ps.setString(4, s.translit()); // no English meaning in the source set; translit stands in
                ps.setString(5, s.place().name());
                ps.setShort(6, (short) s.number());   // revelation_order placeholder = mushaf order
                ps.setShort(7, (short) s.ayahCount());
                ps.setBoolean(8, s.number() != 9);     // At-Tawbah (9) has no Basmala
            }
            public int getBatchSize() { return surahRows.size(); }
        });

        // Verse ids are generated up front so the translations can reference them.
        UUID[] verseIds = new UUID[verseRows.size()];
        for (int i = 0; i < verseIds.length; i++) verseIds[i] = UUID.randomUUID();

        jdbc.batchUpdate("""
                INSERT INTO verse (id, surah_number, ayah_number, text_uthmani, text_simple)
                VALUES (?, ?, ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                VerseRegistrar.VerseRow v = verseRows.get(i);
                ps.setObject(1, verseIds[i]);
                ps.setShort(2, (short) v.surah());
                ps.setShort(3, (short) v.ayah());
                ps.setString(4, v.uthmani());
                ps.setString(5, v.uthmani()); // imla-i simple text not in source; Uthmani stands in
            }
            public int getBatchSize() { return verseRows.size(); }
        });

        jdbc.batchUpdate("""
                INSERT INTO verse_translation (id, verse_id, locale, translator, text, licence)
                VALUES (?, ?, 'en', ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, verseIds[i]);
                ps.setString(3, translator);
                ps.setString(4, verseRows.get(i).translation());
                ps.setString(5, "Tanzil (CC BY 3.0) Arabic; translation under its own licence");
            }
            public int getBatchSize() { return verseRows.size(); }
        });
    }

    @Override
    public void upsertSurah(int number, String nameAr, String nameTranslit, String nameEn,
                            RevelationPlace place, int revelationOrder, int ayahCount) {
        SurahJpaEntity s = surahs.findById((short) number).orElseGet(() -> new SurahJpaEntity((short) number));
        s.setNameAr(nameAr);
        s.setNameTranslit(nameTranslit);
        s.setNameEn(nameEn);
        s.setRevelationPlace(place);
        s.setRevelationOrder((short) revelationOrder);
        s.setAyahCount((short) ayahCount);
        surahs.save(s);
    }

    @Override
    public UUID upsertVerse(int surahNumber, int ayahNumber, String textUthmani, String textSimple) {
        return verses.findBySurahNumberAndAyahNumber((short) surahNumber, (short) ayahNumber)
                .map(existing -> {
                    existing.setTextUthmani(textUthmani);
                    existing.setTextSimple(textSimple);
                    return verses.save(existing).getId();
                })
                .orElseGet(() -> verses.save(
                        VerseJpaEntity.create((short) surahNumber, (short) ayahNumber, textUthmani, textSimple)).getId());
    }

    @Override
    public void upsertTranslation(UUID verseId, String locale, String translator, String text) {
        translations.findFirstByVerseIdAndLocale(verseId, locale).ifPresentOrElse(
                existing -> existing.setText(text),
                () -> translations.save(VerseTranslationJpaEntity.create(verseId, locale, translator, text)));
    }

    @Override
    public Optional<VerseData> findById(UUID verseId, String locale) {
        return verses.findById(verseId).map(v -> toData(v, locale));
    }

    @Override
    public Optional<VerseData> findByRef(int surahNumber, int ayahNumber, String locale) {
        return verses.findBySurahNumberAndAyahNumber((short) surahNumber, (short) ayahNumber)
                .map(v -> toData(v, locale));
    }

    private VerseData toData(VerseJpaEntity v, String locale) {
        SurahJpaEntity s = surahs.findById(v.getSurahNumber()).orElse(null);
        VerseTranslationJpaEntity t = translations.findFirstByVerseIdAndLocale(v.getId(), locale)
                .or(() -> translations.findFirstByVerseId(v.getId()))
                .orElse(null);
        return new VerseData(v.getId(), v.getSurahNumber(), v.getAyahNumber(),
                s == null ? null : s.getNameEn(), s == null ? null : s.getNameAr(),
                v.getTextUthmani(),
                t == null ? null : t.getText(), t == null ? null : t.getTranslator());
    }
}
