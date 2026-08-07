package com.seerah.provenance.adapter.out.persistence;

import com.seerah.shared.HadithGrade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

/**
 * A structured reference into a {@link SourceJpaEntity} (§12.6 {@code citation}):
 * a work, a locator within it, and an optional verbatim quote. Not a footnote
 * string — a first-class row that can be linked to many claims.
 */
@Entity
@Table(name = "citation")
public class CitationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(nullable = false)
    private String locator;

    @Column(name = "locator_kind", nullable = false)
    private String locatorKind = "PAGE";

    private String quote;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "hadith_grade")
    private HadithGrade grade;

    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected CitationJpaEntity() { }

    public static CitationJpaEntity create(UUID sourceId, String locator, String locatorKind,
                                           String quote, HadithGrade grade) {
        CitationJpaEntity c = new CitationJpaEntity();
        c.id = UUID.randomUUID();
        c.sourceId = sourceId;
        c.locator = locator;
        if (locatorKind != null) c.locatorKind = locatorKind;
        c.quote = quote;
        c.grade = grade;
        return c;
    }

    public UUID getId() { return id; }
    public UUID getSourceId() { return sourceId; }
    public String getLocator() { return locator; }
    public String getQuote() { return quote; }
    public HadithGrade getGrade() { return grade; }
}
