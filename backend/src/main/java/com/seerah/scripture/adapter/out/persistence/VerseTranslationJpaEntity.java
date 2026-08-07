package com.seerah.scripture.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** A named rendering of a verse's meaning — never presented as the Quran itself (§12.7). */
@Entity
@Table(name = "verse_translation")
public class VerseTranslationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "verse_id", nullable = false) private UUID verseId;
    @Column(nullable = false) private String locale;
    @Column(nullable = false) private String translator;
    @Column(nullable = false) private String text;
    private String footnotes;
    @Column(nullable = false) private String licence = "UNKNOWN";

    protected VerseTranslationJpaEntity() { }

    public static VerseTranslationJpaEntity create(UUID verseId, String locale, String translator, String text) {
        VerseTranslationJpaEntity t = new VerseTranslationJpaEntity();
        t.id = UUID.randomUUID();
        t.verseId = verseId;
        t.locale = locale;
        t.translator = translator;
        t.text = text;
        return t;
    }

    public String getText() { return text; }
    public void setText(String v) { this.text = v; }
    public String getTranslator() { return translator; }
}
