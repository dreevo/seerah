package com.seerah.content.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Persistence model for {@code chronicle} (§23.3). */
@Entity
@Table(name = "chronicle")
public class ChronicleJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "citext")
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "title_ar")
    private String titleAr;

    private String subtitle;
    private String blurb;
    private String glyph;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private int ordinal;

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getTitleAr() { return titleAr; }
    public String getSubtitle() { return subtitle; }
    public String getBlurb() { return blurb; }
    public String getGlyph() { return glyph; }
    public String getKind() { return kind; }
    public int getOrdinal() { return ordinal; }
}
