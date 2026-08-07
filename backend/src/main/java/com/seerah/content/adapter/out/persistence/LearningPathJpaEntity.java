package com.seerah.content.adapter.out.persistence;

import com.seerah.shared.ContentStatus;
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

/** A curated learning path (§12.8). Title/blurb live in translation. */
@Entity
@Table(name = "learning_path")
public class LearningPathJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "citext")
    private String slug;

    @Column(nullable = false)
    private String audience = "GENERAL";

    @Column(name = "est_minutes")
    private Short estMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "content_status")
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected LearningPathJpaEntity() { }

    public static LearningPathJpaEntity create(String slug, String audience, Integer estMinutes) {
        LearningPathJpaEntity p = new LearningPathJpaEntity();
        p.id = UUID.randomUUID();
        p.slug = slug;
        if (audience != null) p.audience = audience;
        p.estMinutes = estMinutes == null ? null : estMinutes.shortValue();
        return p;
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getAudience() { return audience; }
    public Short getEstMinutes() { return estMinutes; }
}
