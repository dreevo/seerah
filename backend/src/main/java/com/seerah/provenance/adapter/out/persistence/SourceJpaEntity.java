package com.seerah.provenance.adapter.out.persistence;

import com.seerah.shared.SourceTier;
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

/** A cited work (§12.6 {@code source}). A citation is a structured reference to one of these. */
@Entity
@Table(name = "source")
public class SourceJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "citext")
    private String slug;

    @Column(name = "work_title", nullable = false)
    private String workTitle;

    @Column(name = "work_title_ar")
    private String workTitleAr;

    private String author;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "source_tier")
    private SourceTier tier;

    @Column(nullable = false)
    private String licence = "UNKNOWN";

    @Column(name = "is_quotable", nullable = false)
    private boolean quotable;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SourceJpaEntity() { }

    public static SourceJpaEntity create(String slug, String workTitle, String author,
                                         SourceTier tier, boolean quotable) {
        SourceJpaEntity s = new SourceJpaEntity();
        s.id = UUID.randomUUID();
        s.slug = slug;
        s.workTitle = workTitle;
        s.author = author;
        s.tier = tier;
        s.quotable = quotable;
        return s;
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getWorkTitle() { return workTitle; }
    public SourceTier getTier() { return tier; }
    public boolean isQuotable() { return quotable; }
}
