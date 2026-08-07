package com.seerah.scripture.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Reference data: one verse, verbatim Uthmani. Never transformed (§12.7). */
@Entity
@Table(name = "verse")
public class VerseJpaEntity {

    @Id
    private UUID id;

    @Column(name = "surah_number", nullable = false) private Short surahNumber;
    @Column(name = "ayah_number", nullable = false) private Short ayahNumber;
    @Column(name = "text_uthmani", nullable = false) private String textUthmani;
    @Column(name = "text_simple", nullable = false) private String textSimple;
    private Short juz;
    @Column(nullable = false) private boolean sajdah;

    protected VerseJpaEntity() { }

    public static VerseJpaEntity create(short surah, short ayah, String uthmani, String simple) {
        VerseJpaEntity v = new VerseJpaEntity();
        v.id = UUID.randomUUID();
        v.surahNumber = surah;
        v.ayahNumber = ayah;
        v.textUthmani = uthmani;
        v.textSimple = simple;
        return v;
    }

    public UUID getId() { return id; }
    public Short getSurahNumber() { return surahNumber; }
    public Short getAyahNumber() { return ayahNumber; }
    public String getTextUthmani() { return textUthmani; }
    public void setTextUthmani(String v) { this.textUthmani = v; }
    public void setTextSimple(String v) { this.textSimple = v; }
}
